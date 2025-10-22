package org.example.lastcall.domain.bid.repository;

import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	@Query("SELECT MAX(b.bidAmount) FROM Bid b WHERE b.auction = :auction")
	Optional<Long> findMaxBidAmountByAuction(@Param("auction") Auction auction);

	@EntityGraph(attributePaths = {"user"})
	Page<Bid> findAllByAuction(Auction auction, Pageable pageable);

	boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

	@Query("SELECT b FROM Bid b WHERE b.auction = :auction ORDER BY b.bidAmount DESC LIMIT 1 OFFSET 1")
	Optional<Bid> findPreviousHighestBidByAuction(@Param("auction") Auction auction);

	// @Query("SELECT b FROM Bid b WHERE b.auction = :auction ORDER BY b.bidAmount DESC LIMIT 1")
	// TODO: 이후에 주석 삭제
	Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);
}
