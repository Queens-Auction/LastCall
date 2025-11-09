package org.example.lastcall.domain.auction.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.AuctionConfig;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service  // 메시지 수신 역할 하는 서비스
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {
    private final AuctionCommandService auctionCommandService;

    // 큐에 쌓인 경매 종료 이벤트를 자동으로 수신하여 처리
    @RabbitListener(queues = AuctionConfig.END_QUEUE_NAME)
    /*public void handleAuctionEnd(AuctionEvent event) {
        // 메시지 들어오면 실행되는 부분 -> 테스트 후 삭제하기
        System.out.println("경매 종료 이벤트 수신: " + event);
        // 포인트 반환, 낙찰자 확정 등 처리 로직 추가 예정
        // 1. 경매 상태 변경 (closed)
        auctionCommandService.closeAuction(event.getAuctionId());
    }*/
    public void handleAuctionEnd(AuctionEvent event) {
        try {
            System.out.println("경매 종료 이벤트 수신: " + event);
            auctionCommandService.closeAuction(event.getAuctionId());
        } catch (Exception e) {
            System.out.println("메시지 처리 실패 - auctionId: " + event.getAuctionId());
            e.printStackTrace(); // 메시지를 재큐잉 하지 않도록 예외 소비
        }
    }
}
