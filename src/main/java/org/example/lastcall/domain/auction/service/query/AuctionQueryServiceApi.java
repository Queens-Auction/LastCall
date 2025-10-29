package org.example.lastcall.domain.auction.service.query;

import org.example.lastcall.domain.auction.entity.Auction;

public interface AuctionQueryServiceApi {
    // 상품 수정시, 연결 경매 상태 확인 후 수정 가능 여부 검증
    void validateAuctionStatusForModification(Long productId);

    // 입찰 가능한 경매 여부 검증
    Auction getBiddableAuction(Long auctionId);
}
