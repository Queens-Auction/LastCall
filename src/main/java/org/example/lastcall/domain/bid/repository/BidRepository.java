package org.example.lastcall.domain.bid.repository;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    @Query("SELECT MAX(b.bidAmount) FROM Bid b WHERE b.auction = :auction")
    Optional<Long> findMaxBidAmountByAuction(@Param("auction") Auction auction);

    @EntityGraph(attributePaths = {"user"})
    Page<Bid> findAllByAuction(Auction auction, Pageable pageable);

    boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

    @Query("SELECT b FROM Bid b WHERE b.auction = :auction ORDER BY b.bidAmount DESC LIMIT 1 OFFSET 1")
    Optional<Bid> findPreviousHighestBidByAuction(@Param("auction") Auction auction);

    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

    Optional<Bid> findTopByAuctionIdAndUserIdOrderByBidAmountDesc(Long auctionId, Long userId);

    Optional<Bid> findTopByAuctionIdAndUserIdAndIdNotOrderByBidAmountDesc(Long auctionId, Long userId, Long currentBidId);

    @Query("SELECT DISTINCT b.auction.id FROM Bid b WHERE b.user.id = :userId")
    List<Long> findDistinctAuctionsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT b.user.id) FROM Bid b WHERE b.auction.id = :auctionId")
    int countDistinctByAuctionId(Long auctionId);

    @Query("SELECT b FROM Bid b JOIN FETCH b.user WHERE b.auction.id = :auctionId")
    List<Bid> findAllByAuctionId(@Param("auctionId") Long auctionId);
}
