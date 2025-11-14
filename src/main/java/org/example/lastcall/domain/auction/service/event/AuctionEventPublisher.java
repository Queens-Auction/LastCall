package org.example.lastcall.domain.auction.service.event;

import org.example.lastcall.common.config.AuctionConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    // 경매 시작 이벤트를 큐로 발행하는 메서드
    public void sendAuctionStartEvent(AuctionEvent event, Long delayMillis) {
        rabbitTemplate.convertAndSend(
                AuctionConfig.EXCHANGE_NAME,
                AuctionConfig.START_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setHeader("x-delay", delayMillis);

                    return message;
                });
        log.info("[RabbitMQ] 경매 시작 이벤트 발행: {}", event);
    }

    // 경매 종료 이벤트를 큐로 발행하는 메서드
    public void sendAuctionEndEvent(AuctionEvent event, Long delayMillis) {
        rabbitTemplate.convertAndSend(
                AuctionConfig.EXCHANGE_NAME,
                AuctionConfig.END_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties().setHeader("x-delay", delayMillis);

                    return message;
                });
        log.info("[RabbitMQ] 경매 종료 이벤트 발행: {}", event);
    }
}
