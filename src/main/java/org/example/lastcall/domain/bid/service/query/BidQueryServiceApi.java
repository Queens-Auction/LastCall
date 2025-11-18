package org.example.lastcall.domain.bid.service.query;

import java.util.List;
import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;

public interface BidQueryServiceApi {
	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

	Long findCurrentBidAmount(Long auctionId);

	Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

	Optional<Bid> findLastBidExceptBidId(Long auctionId, Long userId, Long currentBidId);

	Bid findById(Long bidId);

	List<Bid> findAllByAuctionId(Long auctionId);
}
