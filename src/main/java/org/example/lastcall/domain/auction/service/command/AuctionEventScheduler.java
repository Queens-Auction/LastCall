package org.example.lastcall.domain.auction.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.event.AuctionEvent;
import org.example.lastcall.domain.auction.service.event.AuctionEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionEventScheduler {
    private final AuctionEventPublisher auctionEventPublisher;

    // 경매 시작/종료 이벤트 예약 발행
    public void scheduleAuctionEvents(Auction auction) {
        long startDelay = Math.max(0, Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis());
        long endDelay = Math.max(0, Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis());

        AuctionEvent startEvent = new AuctionEvent(
                auction.getId(),
                null,
                null,
                null,
                auction.getEventVersion());

        AuctionEvent endEvent = new AuctionEvent(
                auction.getId(),
                null,
                null,
                null,
                auction.getEventVersion());

        try {
            auctionEventPublisher.sendAuctionStartEvent(startEvent, startDelay);
            log.info("[RabbitMQ] 경매 시작 이벤트 예약 완료: auctionId={}, delay={}ms", auction.getId(), startDelay);
        } catch (Exception e) {
            log.error("[RabbitMQ] sendAuctionStartEvent exception: auctionId={}", auction.getId(), e);
        }

        try {
            auctionEventPublisher.sendAuctionEndEvent(endEvent, endDelay);
            log.info("[RabbitMQ] 경매 종료 이벤트 예약 완료: auctionId={}, delay={}ms", auction.getId(), endDelay);
        } catch (Exception e) {
            log.error("[RabbitMQ] sendAuctionEndEvent exception: auctionId={}", auction.getId(), e);
        }
    }

    // 수정 시 재발행 (기존 예약을 새로 덮어쓰기)
    public void rescheduleAuctionEvents(Auction auction) {
        log.info("[RabbitMQ] 경매 이벤트 재발행: auctionId={}", auction.getId());
        scheduleAuctionEvents(auction);
    }
}

