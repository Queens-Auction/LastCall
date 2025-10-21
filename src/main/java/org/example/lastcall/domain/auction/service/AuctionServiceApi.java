package org.example.lastcall.domain.auction.service;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.entity.Auction;

public interface AuctionServiceApi {
    AuctionCreateResponse createAuction(Long userId, AuctionCreateRequest request);

    // 상품에 진행 중인 경매 여부 검증
    void validateAuctionScheduled(Long productId);

    // 입찰 가능한 경매 여부 검증
    Auction getBiddableAuction(Long auctionId);
}
