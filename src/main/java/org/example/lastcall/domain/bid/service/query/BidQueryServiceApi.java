package org.example.lastcall.domain.bid.service.query;

import java.util.List;
import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.Query;

public interface BidQueryServiceApi {
	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

	Long findCurrentBidAmount(Long auctionId);

	Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

	Optional<Bid> findLastBidExceptBidId(Long auctionId, Long userId, Long currentBidId);

	Bid findById(Long bidId);

	@Query("SELECT b FROM Bid b " +
		"WHERE b.bidAmount IN (" +
		"   SELECT MAX(b2.bidAmount) FROM Bid b2 " +
		"   WHERE b2.auction.id = :auctionId " +
		"   GROUP BY b2.user.id" +
		") AND b.auction.id = :auctionId")
	List<Bid> findAllByAuctionId(Long auctionId);
}
