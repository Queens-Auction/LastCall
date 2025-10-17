package org.example.lastcall.domain.bid.repository;

import org.example.lastcall.domain.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
}
