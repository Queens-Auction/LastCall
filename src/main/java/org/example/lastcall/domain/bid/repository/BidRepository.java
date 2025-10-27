package org.example.lastcall.domain.bid.repository;

import java.util.List;
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

	Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

	// 특정 유저의 특정 경매 내 최고 입찰 1건 조회
	Optional<Bid> findTopByAuctionIdAndUserIdOrderByBidAmountDesc(Long auctionId, Long userId);

	Optional<Bid> findTopByAuctionIdAndUserIdAndIdNotOrderByBidAmountDesc(
		Long auctionId,
		Long userId,
		Long currentBidId);

	// 특정 유저가 입찰한 모든 경매의 ID 목록 조회 (중복 제거)
	@Query("SELECT DISTINCT b.auction.id FROM Bid b WHERE b.user.id = :userId")
	List<Long> findDistinctAuctionsByUserId(@Param("userId") Long userId);

	// 특정 경매의 참여자 수 (입찰자 수) 조회
	int countDistinctByAuctionId(Long auctionId);
}