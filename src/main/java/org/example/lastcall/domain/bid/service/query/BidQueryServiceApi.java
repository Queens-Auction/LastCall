package org.example.lastcall.domain.bid.service.query;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;

import java.util.List;
import java.util.Optional;

public interface BidQueryServiceApi {
    boolean existsByAuctionIdAndUserId(Long auctionId, Long userId);

    Long getCurrentBidAmount(Long auctionId);

    Optional<Bid> findPreviousHighestBidByAuction(Auction auction);

    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

    Optional<Bid> findLastBidExceptBidId(
            Long auctionId,
            Long userId,
            Long currentBidId);

    Bid findById(Long bidId);

    List<Long> getParticipatedAuctionIds(Long userId);

    Long getMyBidAmount(Long auctionId, Long userId);

    boolean isUserLeading(Long auctionId, Long userId);

    int countDistinctParticipants(Long auctionId);

    List<Bid> findAllByAuctionId(Long auctionId);
}
