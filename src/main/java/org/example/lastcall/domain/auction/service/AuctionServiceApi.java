package org.example.lastcall.domain.auction.service;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.entity.Auction;

public interface AuctionServiceApi {

    AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request);

    // 상품 수정시, 연결 경매 상태 확인 후 수정 가능 여부 검증
    void validateAuctionStatusForModification(Long productId);

    // 입찰 가능한 경매 여부 검증
    Auction getBiddableAuction(Long auctionId);

    // 경매 ID 기준으로 특정 경매 엔티티 조회
    Auction findById(Long auctionId);
}
