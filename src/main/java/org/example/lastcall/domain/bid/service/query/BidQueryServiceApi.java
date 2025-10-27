package org.example.lastcall.domain.bid.service.query;

import java.util.List;
import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;

public interface BidQueryServiceApi {
	// 특정 경매에 해당 사용자의 입찰 존재 여부 확인
	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

	// 특정 경매의 최고 입찰가 조회
	Long getCurrentBidAmount(Long auctionId);

	// 이전 최고 입찰자, 최고 입찰가 조회
	Optional<Bid> findPreviousHighestBidByAuction(Auction auction);

	// 최고 입찰자, 최고 입찰가 조회
	Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

	// 특정 경매에서 특정 유저의 마지막 입찰 기록 조회
	Optional<Bid> findLastBidExceptBidId(
		Long auctionId,
		Long userId,
		Long currentBidId);

	// ID로 입찰 단건 조회
	Bid findById(Long bidId);

	// 특정 유저가 입찰한 경매 목록 조회
	List<Long> getParticipatedAuctionIds(Long userId);

	// 특정 경매에서 특정 유저의 입찰 최고가 조회
	Long getMyBidAmount(Long auctionId, Long userId);

	// 특정 유저가 특정 경매에서 최고 입찰자인지 여부 조회
	boolean isUserLeading(Long auctionId, Long userId);

	// 특정 경매 참여자(입찰자) 수 조회
	int countDistinctParticipants(Long auctionId);
}
