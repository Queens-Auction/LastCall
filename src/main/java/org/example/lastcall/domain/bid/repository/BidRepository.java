package org.example.lastcall.domain.bid.repository;

import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	@Query("SELECT MAX(b.bidAmount) FROM Bid b WHERE b.auction = :auction")
	Optional<Long> findMaxBidAmountByAuction(@Param("auction") Auction auction);
}
