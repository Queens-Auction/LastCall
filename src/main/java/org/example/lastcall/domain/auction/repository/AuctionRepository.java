package org.example.lastcall.domain.auction.repository;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    // 재등록이 안되는 경매 조건
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

    // 경매 전체 조회 (기본값(최신순조회), 마감임박순, 인기순, 카테고리순)
    // 추후 Slice 고려
    // 경매 전체 조회는 마감된 경매 제외
    @Query("SELECT a " +
            "FROM Auction a " +
            "WHERE a.status IN('ONGOING', 'SCHEDULED') " +
            "AND (:category is NULL OR a.product.category = :category)")
    Page<Auction> findAllActiveAuctionsByCategory(
            @Param("category") Category category,
            Pageable pageable);

    // 상품에 진행 중인 경매 존재 여부 검증
    boolean existsByProductIdAndStatus(Long productId, AuctionStatus status);

    // 특정 이메일을 가진 판매자가 등록한 경매 목록 조회 (우선 이메일로)
    // 추후 findBySellerId(Long sellerId, Pageable pageable); 로 변경 예정
    Page<Auction> findBySellerEmail(String email, Pageable pageable);
}
