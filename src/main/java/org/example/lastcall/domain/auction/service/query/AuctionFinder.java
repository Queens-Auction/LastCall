package org.example.lastcall.domain.auction.service.query;

import org.example.lastcall.domain.auction.entity.Auction;

public interface AuctionFinder {
    Auction findById(Long auctionId);
}
