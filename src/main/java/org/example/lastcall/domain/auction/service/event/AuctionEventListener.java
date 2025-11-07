package org.example.lastcall.domain.auction.service.event;

import org.example.lastcall.common.config.AuctionConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service  // 메시지 수신 역할 하는 서비스
public class AuctionEventListener {
    // 큐에 쌓인 경매 종료 이벤트를 자동으로 수신하여 처리
    @RabbitListener(queues = AuctionConfig.QUEUE_NAME)
    public void handleAuctionEnd(AuctionEvent event) {
        // 메시지 들어오면 실행되는 부분
        System.out.println("경매 종료 이벤트 수신: " + event);
        // 포인트 반환, 낙찰자 확정 등 처리 로직 추가 예정
    }
}
