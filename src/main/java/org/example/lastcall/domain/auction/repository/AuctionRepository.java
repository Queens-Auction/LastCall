package org.example.lastcall.domain.auction.repository;

import org.example.lastcall.domain.auction.entity.Auction;
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
}
