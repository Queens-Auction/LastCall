package org.example.lastcall.domain.bid.service.query;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.query.AuctionFinder;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidQueryService implements BidQueryServiceApi {
    private final BidRepository bidRepository;
    private final AuctionFinder auctionFinder;

    public PageResponse<BidGetAllResponse> getAllBids(Long auctionId, Pageable pageable) {
        Auction auction = auctionFinder.findById(auctionId);
        Page<Bid> bidPage = bidRepository.findAllByAuction(auction, pageable);

        return PageResponse.of(bidPage.map(BidGetAllResponse::from));
    }

    @Override
    public boolean existsByAuctionIdAndUserId(Long auctionId, Long userId) {
        return bidRepository.existsByAuctionIdAndUserId(auctionId, userId);
    }

    @Override
    public Long getCurrentBidAmount(Long auctionId) {
        Auction auction = auctionFinder.findById(auctionId);

        return bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());
    }

    @Override
    public Optional<Bid> findPreviousHighestBidByAuction(Auction auction) {
        return bidRepository.findPreviousHighestBidByAuction(auction);
    }

    @Override
    public Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction) {
        return bidRepository.findTopByAuctionOrderByBidAmountDesc(auction);
    }

    @Override
    public Optional<Bid> findLastBidExceptBidId(Long auctionId, Long userId, Long currentBidId) {
        return bidRepository.findTopByAuctionIdAndUserIdAndIdNotOrderByBidAmountDesc(auctionId, userId, currentBidId);
    }

    @Override
    public Bid findById(Long bidId) {
        return bidRepository.findById(bidId).orElseThrow(
                () -> new BusinessException(BidErrorCode.BID_NOT_FOUND)
        );
    }

    @Override
    public List<Long> getParticipatedAuctionIds(Long userId) {
        return bidRepository.findDistinctAuctionsByUserId(userId);
    }

    @Override
    public boolean isUserLeading(Long auctionId, Long userId) {
        Long currentBid = getCurrentBidAmount(auctionId);
        Long myBidAmount = getMyBidAmount(auctionId, userId);

        return myBidAmount != null && myBidAmount.equals(currentBid);
    }

    @Override
    public Long getMyBidAmount(Long auctionId, Long userId) {
        return bidRepository.findTopByAuctionIdAndUserIdOrderByBidAmountDesc(auctionId, userId)
                .map(Bid::getBidAmount)
                .orElse(null);
    }

    @Override
    public int countDistinctParticipants(Long auctionId) {
        return bidRepository.countDistinctByAuctionId(auctionId);
    }

    @Override
    public List<Bid> findAllByAuctionId(Long auctionId) {
        return bidRepository.findAllByAuctionId(auctionId);
    }
}
