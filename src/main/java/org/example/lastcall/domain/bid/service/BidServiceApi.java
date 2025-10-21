package org.example.lastcall.domain.bid.service;

public interface BidServiceApi {
	// 특정 경매에 해당 사용자의 입찰 존재 여부 확인
	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);
}
