package org.example.lastcall.domain.auction.repository;

import java.util.Optional;

import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long>, AuctionQueryRepository {
	// 경매 재등록 가능 여부 검증
	@Query("SELECT COUNT(a) > 0 " +
		"FROM Auction a " +
		"WHERE a.product.id = :productId " +
		"AND(" +
		"a.status = 'SCHEDULED'" +
		"OR a.status = 'ONGOING'" +
		"OR (a.status = 'CLOSED' AND a.currentBid IS NOT NULL)" +
		" )"
	)
	boolean existsActiveAuction(@Param("productId") Long productId);

	// 내가 판매한 경매 목록 조회 (페이징)
	@Query("""
		       SELECT new org.example.lastcall.domain.auction.dto.response.MySellingResponse(
		            a.id,
		                (
		                    SELECT i.imageKey
		                    FROM ProductImage i
		                    WHERE i.product.id = a.product.id
		                      AND i.imageType = 'THUMBNAIL'
		                      AND i.deleted = false
		                ),
		                p.name,
		                p.description,
		                COALESCE((
		                    SELECT MAX(b.bidAmount)
		                    FROM Bid b
		                    WHERE b.auction.id = a.id
		                ), 0),
		                a.status,
		                a.startTime,
		                a.endTime
		            )
		            FROM Auction a
		            JOIN a.product p
		            WHERE a.user.id = :userId
		              AND a.deleted = false
		            ORDER BY a.createdAt DESC
		""")
	Page<MySellingResponse> findMySellingAuctions(@Param("userId") Long userId, Pageable pageable);

	// 내가 판매한 특정 경매 단건 조회
	@Query("SELECT a FROM Auction a WHERE a.user.id = :userId AND a.id = :auctionId")
	Optional<Auction> findBySellerIdAndAuctionId(Long userId, Long auctionId);

	// 상품 ID로 연결된 경매 조회
	@Query("SELECT a FROM Auction a WHERE a.product.id = :productId AND a.deleted = false")
	Optional<Auction> findByProductId(Long productId);

	// 삭제된 경매는 제외하고 조회
	@Query("SELECT a FROM Auction a WHERE a.id = :auctionId AND a.deleted = false")
	Optional<Auction> findActiveById(Long auctionId);
}
