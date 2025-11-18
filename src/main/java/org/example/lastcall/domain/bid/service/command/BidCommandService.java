package org.example.lastcall.domain.bid.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.lock.DistributedLock;
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
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BidCommandService {
    private final BidRepository bidRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserQueryServiceApi userQueryServiceApi;
    private final PointCommandServiceApi pointCommandServiceApi;
    private final PointQueryServiceApi pointQueryServiceApi;

    @DistributedLock(key = "'auction:' + #auctionId")
    public BidResponse createBid(Long auctionId, AuthUser authUser, Long nextBidAmount) {
        log.debug("락 획득 후 작업 실행: 입찰 요청 - auctionId={}, userId={}, nextBidAmount={}", auctionId, authUser.userId(), nextBidAmount);

        Auction auction = auctionQueryServiceApi.findBiddableAuction(auctionId);

        if (auction.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(BidErrorCode.SELLER_CANNOT_BID);
        }

        User user = userQueryServiceApi.findById(authUser.userId());

        boolean alreadyParticipated = bidRepository.existsByAuctionIdAndUserId(auction.getId(), user.getId());

        Long currentMaxBid =
                bidRepository.findMaxBidAmountByAuction(auction)
                        .orElse(auction.getStartingBid());

        Long expectedNextBidAmount = currentMaxBid + auction.getBidStep();

        if (nextBidAmount < expectedNextBidAmount) {
            throw new BusinessException(BidErrorCode.CONCURRENCY_BID_FAILED);
        } else if (!nextBidAmount.equals(expectedNextBidAmount)) {
            throw new BusinessException(BidErrorCode.INVALID_BID_AMOUNT);
        }

        pointQueryServiceApi.validateSufficientPoints(user.getId(), nextBidAmount);
        log.debug("포인트 검증 완료 - userId={}, nextBidAmount={}", user.getId(), nextBidAmount);

        Bid bid = Bid.of(nextBidAmount, auction, user);
        Bid savedBid = bidRepository.save(bid);

        if (!alreadyParticipated) {
            auction.incrementParticipantCount();
        }

        auction.updateCurrentBid(nextBidAmount);

        try {
            pointCommandServiceApi.updateDepositPoint(auction.getId(), savedBid.getId(), nextBidAmount, user.getId());
        } catch (BusinessException e) {
            throw e;
        }

        log.debug("입찰 생성 완료 - auctionId={}, userId={}, nextBidAmount={}", auctionId, user.getId(), nextBidAmount);

        return BidResponse.from(savedBid);
    }
}
