package org.example.lastcall.domain.auction.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuctionStatus {
    SCHEDULED,
    ONGOING,
    CLOSED
}