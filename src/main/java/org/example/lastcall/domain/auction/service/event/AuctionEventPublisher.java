package org.example.lastcall.domain.auction.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.AuctionConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventPublisher {
    // RabbitMQ로 메시지 전송 시 사용하는 템플릿
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
                }
        );
        log.info("[RabbitMQ] 경매 시작 이벤트 발행: {}", event);
    }

    // 경매 종료 이벤트를 큐로 발행하는 메서드
    public void sendAuctionEndEvent(AuctionEvent event, Long delayMillis) {
        // Exchange 와 RoutingKey를 지정하여 메시지 전송
        rabbitTemplate.convertAndSend(
                AuctionConfig.EXCHANGE_NAME,      // 대상 교환기 이름
                AuctionConfig.END_ROUTING_KEY,    // 라우팅 키
                event,                            // 메시지 본문 (AuctionEvent 객체)
                message -> {
                    // 메시지 헤더에 지연 시간 설정
                    // delayMillis = 지연 시간 -> 메시지를 큐로 보내기 전 기다릴 시간(밀리 초)
                    message.getMessageProperties().setHeader("x-delay", delayMillis);
                    return message;
                }
        );
        log.info("[RabbitMQ] 경매 종료 이벤트 발행: {}", event);
    }
}
