package org.example.lastcall.domain.bid.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.service.command.PointCommandServiceApi;
import org.example.lastcall.domain.point.service.query.PointQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BidCommandService {
    private final BidRepository bidRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserServiceApi userServiceApi;
    private final PointCommandServiceApi pointCommandServiceApi;
    private final PointQueryServiceApi pointQueryServiceApi;

    public BidResponse createBid(Long auctionId, AuthUser authUser) {
        Auction auction = auctionQueryServiceApi.getBiddableAuction(auctionId);

        if (auction.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(BidErrorCode.SELLER_CANNOT_BID);
        }

        User user = userServiceApi.findById(authUser.userId());

        boolean alreadyParticipated = bidRepository.existsByAuctionIdAndUserId(
                auction.getId(),
                user.getId()
        );

        Long currentMaxBid = bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());
        Long bidAmount = currentMaxBid + auction.getBidStep();

        pointQueryServiceApi.validateSufficientPoints(user.getId(), bidAmount);

        Bid bid = Bid.of(bidAmount, auction, user);
        Bid savedBid = bidRepository.save(bid);

        if (!alreadyParticipated) {
            auction.incrementParticipantCount();
        }

        auction.updateCurrentBid(bidAmount);

        pointCommandServiceApi.updateDepositPoint(
                auction.getId(),
                savedBid.getId(),
                bidAmount,
                user.getId());

        return BidResponse.from(savedBid);
    }
}
