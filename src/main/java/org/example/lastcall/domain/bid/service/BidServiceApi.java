package org.example.lastcall.domain.bid.service;

import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;

public interface BidServiceApi {
	// 특정 경매에 해당 사용자의 입찰 존재 여부 확인
	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

	// 특정 경매의 최고 입찰가 조회
	Long getCurrentBidAmount(Long auctionId);

	// 이전 최고 입찰자, 최고 입찰가를 찾는 로직
	Optional<Bid> findPreviousHighestBidByAuction(Auction auction);
}
