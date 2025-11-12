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
        // [시작까지 남은 시간 계산]
        long startDelay = Math.max(0,
                Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis());

        // [종료까지 남은 시간 계산]
        long endDelay = Math.max(0,
                Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis());

        // [이벤트 생성]
        AuctionEvent startEvent = new AuctionEvent(
                auction.getId(),
                null,
                null,
                null,
                auction.getVersion()
        );
        AuctionEvent endEvent = new AuctionEvent(
                auction.getId(),
                null,
                null,
                null,
                auction.getVersion()
                );

        // [시작 이벤트 예약 발행]
        auctionEventPublisher.sendAuctionStartEvent(startEvent, startDelay);
        log.info("경매 시작 이벤트 예약 완료 - auctionId={}, delay={}ms", auction.getId(), startDelay);

        // [종료 이벤트 예약 발행]
        auctionEventPublisher.sendAuctionEndEvent(endEvent, endDelay);
        log.info("경매 종료 이벤트 예약 완료 - auctionId={}, delay={}ms", auction.getId(), endDelay);
    }

    // 수정 시 재발행 (기존 예약을 새로 덮어쓰기)
    public void rescheduleAuctionEvents(Auction auction) {
        log.info("경매 수정으로 이벤트 재발행 시작 - auctionId={}", auction.getId());
        scheduleAuctionEvents(auction);
    }
}

