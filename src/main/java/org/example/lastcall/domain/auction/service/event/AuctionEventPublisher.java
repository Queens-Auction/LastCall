package org.example.lastcall.domain.auction.service.event;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.config.AuctionConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionEventPublisher {
    // RabbitMQ로 메시지 전송 시 사용하는 템플릿
    private final RabbitTemplate rabbitTemplate;

    // 경매 종료 이벤트를 큐로 발행하는 메서드
    public void sendAuctionEndEvent(AuctionEvent event, Long delayMillis) {
        // Exchange 와 RoutingKey를 지정하여 메시지 전송
        rabbitTemplate.convertAndSend(
                AuctionConfig.EXCHANGE_NAME,  // 대상 교환기 이름
                AuctionConfig.ROUTING_KEY,    // 라우팅 키
                event,                        // 메시지 본문 (AuctionEvent 객체)
                message -> {
                    // 메시지 헤더에 지연 시간 설정
                    message.getMessageProperties().setHeader("x-delay", delayMillis);
                    return message;
                }
        );
        System.out.println("경매 종료 이벤트 발행: " + event);
    }
}
