package org.example.lastcall.domain.auction.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.AuctionConfig;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Service  // 메시지 수신 역할 하는 서비스
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {
    private final AuctionCommandService auctionCommandService;
    private final AuctionRepository auctionRepository;

    /**
     * [ 경매 시작 이벤트 처리 메서드 ]
     * - START_QUEUE_NAME 큐에 메시지가 들어오면 자동 실행됨
     * - 경매 버전 검증 후 실제 startAuction() 호출
     * - 수동 ACK/NACK을 통해 RabbitMQ 메시지 재전송/삭제를 제어
     */
    @RabbitListener(queues = AuctionConfig.START_QUEUE_NAME)
    public void handleAuctionStart(AuctionEvent event, Message message, Channel channel) {
        processEvent(event, message, channel,
                // 메서드 참조 사용
                auctionCommandService::startAuction,
                "경매 시작");
    }

    /**
     * [ 경매 종료 이벤트 처리 메서드 ]
     * - END_QUEUE_NAME 큐에 메시지가 들어오면 자동 실행됨
     * - 경매 버전 검증 후 실제 closeAuction() 호출
     * - 종료 로직 수행 후 수동 ACK 처리
     */
    @RabbitListener(queues = AuctionConfig.END_QUEUE_NAME)
    public void handleAuctionEnd(AuctionEvent event, Message message, Channel channel) {
        processEvent(event, message, channel,
                auctionCommandService::closeAuction,
                "경매 종료");
    }

    /**
     * [ 공용 이벤트 처리 메서드 ]
     * - 경매 시작/종료 공통 로직 처리
     * - 버전 불일치 시 중복 방지 처리
     * - 비즈니스 예외(사용자 오류)는 재시도 X -> ACK
     * - 시스템 예외는 재시도 O -> NACK
     */
    // 공용 이벤트 처리 헬퍼 메서드 (메서드 분리)
    private void processEvent(AuctionEvent event,
                              Message message,
                              Channel channel,
                              Consumer<Long> auctionAuction,
                              String eventType) {
        try {
            log.info("{} 이벤트 수신: {}", eventType, event);

            // 1. 이벤트의 auctionId로 경매 조회
            Auction auction = auctionRepository.findById(event.getAuctionId()).orElseThrow(
                    () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

            // 2. 버전 불일치 시 메시지 삭제(무시) - 중복 방지
            if (!Objects.equals(auction.getVersion(), event.getVersion())) {
                log.warn("무시된 이벤트 - 경매 버전 불일치 (이벤트 버전={}, 현재 버전={})",
                        event.getVersion(),
                        auction.getVersion());
                ackMessage(channel, message);
                return;
            }
            // 3. 정상적인 경우 비즈니스 로직 실행 (경매 시작 or 종료)
            auctionAuction.accept(event.getAuctionId());

            // 4. 성공 처리 시 ACK
            ackMessage(channel, message);
            log.info("{} 이벤트 처리 완료 - auctionId={}", eventType, event.getAuctionId());

        } catch (BusinessException e) {
            log.warn("{} 비즈니스 예외 발생 - auctionId={}, message={}", eventType, event.getAuctionId(), e.getMessage());
            ackMessage(channel, message);

        } catch (Exception e) {
            log.error("{} 처리 중 시스템 예외 발생 - auctionId={}", eventType, event.getAuctionId(), e);
            nackMessage(channel, message, true);
        }
    }

    // [ 공용 ACK 헬퍼 메서드 ]
    // RabbitMQ에서는 꼭 수동 ack 처리로 메시지 삭제해야 전송 안 됨
    // 버전이 다르더라도 메세지를 큐에서 제거해야 재처리 안 되므로 ACK 호출
    private void ackMessage(Channel channel, Message message) {
        try {
            // 정상 처리 되었으니, 큐에서 삭제해도 돼 라는 의미
            // basicAck(tag, false) : 처리 성공
            // basicAck(tag, false, true) : 처리 성공 + 재시도 원함
            // basicAck(tag, false, false) : 처리 성공 + 재시도 원치 않음
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException ioEx) {
            log.error("RabbitMQ ACK 처리 중 IOException 발생", ioEx);
        }
    }

    // [ 공용 NACK 헬퍼 메서드 ]
    private void nackMessage(Channel channel, Message message, boolean requeue) {
        try {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, requeue);
        } catch (IOException ioEx) {
            log.error("RabbitMQ NACK 처리 중 IOException 발생", ioEx);
        }
    }
}