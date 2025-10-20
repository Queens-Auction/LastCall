package org.example.lastcall.domain.auction.service;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;

public interface AuctionServiceApi {

    // 상품에 진행 중인 경매 여부 검증
    void validateAuctionNotOngoing(Long productId);

    AuctionCreateResponse createAuction(Long userId, AuctionCreateRequest request);
}
