package org.example.lastcall.domain.auction.repository;

import org.example.lastcall.domain.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
