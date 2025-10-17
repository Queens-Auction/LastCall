package org.example.lastcall.domain.auction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuctionStatus {
    STATUS_SCHEDULED(Status.SCHEDULED),
    STATUS_ONGOING(Status.ONGOING),
    STATUS_CLOSED(Status.CLOSED);

    private final String auctionStatus;

    public static class Status {
        public static final String SCHEDULED = "STATUS_SCHEDULED";
        public static final String ONGOING = "STATUS_ONGOING";
        public static final String CLOSED = "STATUS_CLOSED";
    }
}
