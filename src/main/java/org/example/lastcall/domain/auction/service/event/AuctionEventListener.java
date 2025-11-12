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
        try {
            log.info("경매 시작 이벤트 수신: {}", event);

            // 1. 이벤트에 담긴 auctionId로 실제 경매 조회
            Auction auction = auctionRepository.findById(event.getAuctionId()).orElseThrow(
                    () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

            // 2. 이벤트의 버전과 DB의 경매 버전이 불일치 시 처리 X => " 중복 이벤트 방지 "
            if (!Objects.equals(auction.getVersion(), event.getVersion())) {
                log.warn("무시된 이벤트 - 경매 버전 불일치 (이벤트 버전={}, 현재 버전={})",
                        event.getVersion(),
                        auction.getVersion());
                // RabbitMQ에서는 꼭 수동 ack 처리로 메시지 삭제해야 전송 안 됨
                // 버전이 다르더라도 메세지를 큐에서 제거해야 재처리 안 되므로 ACK 호출
                try {
                    // 정상 처리 되었으니, 큐에서 삭제해도 돼 라는 의미
                    // basicAck(tag, false) : 처리 성공
                    // basicAck(tag, false, true) : 처리 성공 + 재시도 원함
                    // basicAck(tag, false, false) : 처리 성공 + 재시도 원치 않음
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException ioEx) {
                    log.error("RabbitMQ ACK 처리 중 IOException", ioEx);
                }
                // 메서드 종료
                return;
            }
            // 3. 정상적인 경우 실제 경매 로직 수행
            auctionCommandService.startAuction(event.getAuctionId());

            // 4. 메시지 정상 처리 됐으므로 수동 ACK 호출 -> 큐에서 메시지 제거됨
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException ioEx) {
                log.error("RabbitMQ ACK 처리 중 IOException 발생", ioEx);
            }

        } catch (Exception e) {
            // 비즈니스 로직 처리 중 예외 발생 시 로그 출력
            log.error("메시지 처리 실패 - auctionId: {}", event.getAuctionId(), e);
            // 예외 발생 시 메시지 재전송(NACK) -> requeue=true 로 설정 시 다시 큐에 들어감
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ioEx) {
                log.error("RabbitMQ NACK 처리 중 IOException 발생", ioEx);
            }
        }
    }

    /**
     * 🎯 경매 종료 이벤트 처리 메서드
     * - END_QUEUE_NAME 큐에 메시지가 들어오면 자동 실행됨
     * - 종료 로직 수행 후 수동 ACK 처리
     */
    @RabbitListener(queues = AuctionConfig.END_QUEUE_NAME)
    public void handleAuctionEnd(AuctionEvent event, Message message, Channel channel) {
        try {
            log.info("경매 종료 이벤트 수신: {}", event);

            // 1. 이벤트에 담긴 auctionId로 실제 경매 조회
            Auction auction = auctionRepository.findById(event.getAuctionId()).orElseThrow(
                    () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

            // 2. 이벤트의 버전과 DB의 경매 버전이 불일치 시 처리 X => " 중복 이벤트 방지 "
            if (!Objects.equals(auction.getVersion(), event.getVersion())) {
                log.warn("무시된 이벤트 - 경매 버전 불일치 (이벤트 버전={}, 현재 버전={})",
                        event.getVersion(),
                        auction.getVersion());
                // RabbitMQ에서는 꼭 수동 ack 처리로 메시지 삭제해야 전송 안 됨
                // 버전이 다르더라도 메세지를 큐에서 제거해야 재처리 안 되므로 ACK 호출
                try {
                    // 정상 처리 되었으니, 큐에서 삭제해도 돼 라는 의미
                    // basicAck(tag, false) : 처리 성공
                    // basicAck(tag, false, true) : 처리 성공 + 재시도 원함
                    // basicAck(tag, false, false) : 처리 성공 + 재시도 원치 않음
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                } catch (IOException ioEx) {
                    log.error("RabbitMQ ACK 처리 중 IOException", ioEx);
                }
                // 메서드 종료
                return;
            }

            // 3. 경매 종료 처리 -> 낙찰자 확정 / 최종 낙찰가 확정 / 포인트 전환
            auctionCommandService.closeAuction(event.getAuctionId());

            // 4. 정상 처리 완료 시 수동 ACK 호출 -> 메시지 큐에서 제거
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException ioEx) {
                log.error("RabbitMQ ACK 처리 중 IOException 발생", ioEx);
            }

        } catch (Exception e) {
            // 비즈니스 로직 처리 중 예외 발생 시 로그 출력
            log.error("메시지 처리 실패 - auctionId: {}", event.getAuctionId(), e);

            // 예외 발생 시 메시지 재전송(NACK) -> requeue=true 로 설정 시 다시 큐에 들어감
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            } catch (IOException ioEx) {
                log.error("RabbitMQ NACK 처리 중 IOException 발생", ioEx);
            }
        }
    }
}
