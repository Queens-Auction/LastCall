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

    // 경매 시작 이벤트 //
    @RabbitListener(queues = AuctionConfig.START_QUEUE_NAME)
    public void handleAuctionStart(AuctionEvent event) {
        try {
            log.info("경매 시작 이벤트 수신: {}", event);
            auctionCommandService.startAuction(event.getAuctionId());
        } catch (Exception e) {
            log.error("메시지 처리 실패 - auctionId: {}", event.getAuctionId(), e);
        }
    }

    // 경매 종료 이벤트 //
    // 큐에 쌓인 경매 종료 이벤트를 자동으로 수신하여 처리
    @RabbitListener(queues = AuctionConfig.END_QUEUE_NAME)
    public void handleAuctionEnd(AuctionEvent event) {
        try {
            log.info("경매 종료 이벤트 수신: {}", event);
            auctionCommandService.closeAuction(event.getAuctionId());
        } catch (Exception e) {
            log.error("메시지 처리 실패 - auctionId: {}", event.getAuctionId(), e);
        }
    }
}
