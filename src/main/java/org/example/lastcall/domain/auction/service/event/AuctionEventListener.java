package org.example.lastcall.domain.auction.service.event;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.AuctionConfig;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {
    private final AuctionCommandService auctionCommandService;
    private final AuctionRepository auctionRepository;

    // 경매 시작 이벤트 처리 메서드
    @RabbitListener(queues = AuctionConfig.START_QUEUE_NAME)
    public void handleAuctionStart(AuctionEvent event, Message message, Channel channel) {
        processEvent(event, message, channel, auctionCommandService::startAuction, "[RabbitMQ] 경매 시작");
    }

    // 경매 종료 이벤트 처리 메서드
    @RabbitListener(queues = AuctionConfig.END_QUEUE_NAME)
    public void handleAuctionEnd(AuctionEvent event, Message message, Channel channel) {
        processEvent(event, message, channel, auctionCommandService::closeAuction, "[RabbitMQ] 경매 종료");
    }

    // 공용 이벤트 처리 헬퍼 메서드 (메서드 분리)
    private void processEvent(AuctionEvent event, Message message, Channel channel, Consumer<Long> auctionAuction, String eventType) {
        try {
            log.info("[RabbitMQ] {} 이벤트 수신: {}", eventType, event);

            // 1. 이벤트의 auctionId로 경매 조회
            Auction auction = auctionRepository.findById(event.getAuctionId()).orElseThrow(
                    () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

            // 2. 버전 불일치 시 메시지 삭제(무시) - 중복 방지
            if (!Objects.equals(auction.getEventVersion(), event.getVersion())) {
                log.warn("[RabbitMQ] 무시된 이벤트 - 경매 버전 불일치 (이벤트 버전={}, 현재 버전={})", event.getVersion(), auction.getEventVersion());
                ackMessage(channel, message);

                return;
            }

            // 3. 정상적인 경우 비즈니스 로직 실행 (경매 시작 or 종료)
            auctionAuction.accept(event.getAuctionId());

            // 4. 성공 처리 시 ACK
            ackMessage(channel, message);
            log.info("[RabbitMQ] {} 이벤트 처리 완료 - auctionId={}", eventType, event.getAuctionId());

        } catch (BusinessException e) {
            log.warn("[RabbitMQ] {} 비즈니스 예외 발생 - auctionId={}, message={}", eventType, event.getAuctionId(), e.getMessage());
            ackMessage(channel, message);
        } catch (Exception e) {
            log.error("[RabbitMQ] {} 처리 중 시스템 예외 발생 - auctionId={}", eventType, event.getAuctionId(), e);
            nackMessage(channel, message, false);
        }
    }

    // 공용 ACK 헬퍼 메서드
    private void ackMessage(Channel channel, Message message) {
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException ioEx) {
            log.error("[RabbitMQ] RabbitMQ ACK 처리 중 IOException 발생", ioEx);
        }
    }

    // 공용 NACK 헬퍼 메서드
    private void nackMessage(Channel channel, Message message, boolean requeue) {
        try {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, requeue);
        } catch (IOException ioEx) {
            log.error("[RabbitMQ] RabbitMQ NACK 처리 중 IOException 발생", ioEx);
        }
    }
}