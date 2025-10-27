package org.example.lastcall.domain.auction.service.query;

import org.example.lastcall.domain.auction.entity.Auction;

public interface AuctionFinder {

    // 경매 ID 기준으로 특정 경매 엔티티 조회
    Auction findById(Long auctionId);
}
