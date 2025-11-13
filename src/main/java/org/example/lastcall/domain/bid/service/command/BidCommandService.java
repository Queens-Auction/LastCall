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
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BidCommandService {
    private final BidRepository bidRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserServiceApi userServiceApi;
    private final PointCommandServiceApi pointCommandServiceApi;
    private final PointQueryServiceApi pointQueryServiceApi;

    // 입찰 등록
    @DistributedLock(key = "'auction:' + #auctionId")
    public BidResponse createBid(Long auctionId, AuthUser authUser) {
        log.debug("[RedissonLock] 락 획득 후 입찰 처리 시작 - auctionId={}, userId={}",
                auctionId, authUser.userId());

        // 입찰이 가능한 경매인지 확인하고, 경매를 받아옴
        log.debug("[RedissonLock] 입찰 가능한 경매 조회 시작 - auctionId={}", auctionId);

        Auction auction = auctionQueryServiceApi.getBiddableAuction(auctionId);

        if (auction.getUser().getId().equals(authUser.userId())) {
            log.warn("판매자가 본인 경매에 입찰 시도 - auctionId={}, userId={}",
                    auctionId, authUser.userId());
            throw new BusinessException(BidErrorCode.SELLER_CANNOT_BID);
        }

        User user = userServiceApi.findById(authUser.userId());
        log.debug("[RedissonLock] 입찰자 조회 완료 - userId={}, nickname={}",
                user.getId(), user.getNickname());

        boolean alreadyParticipated = bidRepository.existsByAuctionIdAndUserId(
                auction.getId(),
                user.getId()
        );
        log.debug("기존 입찰자 여부 확인 - auctionId={}, userId={}, alreadyParticipated={}",
                auctionId, user.getId(), alreadyParticipated);

        Long currentMaxBid = bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());
        Long bidAmount = currentMaxBid + auction.getBidStep();
        log.debug("[RedissonLock] 현재가 기반 입찰가 계산 완료 - auctionId={}, currentMaxBid={}, newBid={}",
                auctionId, currentMaxBid, bidAmount);

        // 입찰 금액 검증 (이상 경쟁 방어)
        if (bidAmount <= currentMaxBid) {
            log.warn("입찰 금액이 현재 최고가 이하임 - auctionId={}, bidAmount={}, currentMaxBid={}",
                    auctionId, bidAmount, currentMaxBid);
            throw new BusinessException(BidErrorCode.BID_AMOUNT_TOO_LOW);
        }

        pointQueryServiceApi.validateSufficientPoints(user.getId(), bidAmount);
        log.debug("포인트 충분 검증 완료 - userId={}, bidAmount={}", user.getId(), bidAmount);

        Bid bid = Bid.of(bidAmount, auction, user);
        Bid savedBid = bidRepository.save(bid);
        //Bid savedBid = bidRepository.save(new Bid(bidAmount, auction, user));
        log.info("[Bid] 입찰 생성 완료 - bidId={}, auctionId={}, userId={}, bidAmount={}",
                savedBid.getId(), auctionId, user.getId(), bidAmount);

        if (!alreadyParticipated) {
            auction.incrementParticipantCount();
            log.debug("[Bid] 신규 입찰자 참여 카운트 증가 - auctionId={}, participants={}",
                    auctionId, auction.getParticipantCount());
        }

        auction.updateCurrentBid(bidAmount);
        log.debug("[Bid] 경매 현재 입찰가 갱신 - auctionId={}, currentBid={}",
                auctionId, bidAmount);

        try {
            pointCommandServiceApi.updateDepositPoint(
                    auction.getId(),
                    savedBid.getId(),
                    bidAmount,
                    user.getId());
            log.debug("[Bid] 포인트 예치 완료 - auctionId={}, userId={}, bidAmount={}", auctionId, user.getId(), bidAmount);
        } catch (BusinessException e) {
            log.error("[Bid] 포인트 예치 중 예외 발생 - auctionId={}, userId={}, message={}",
                    auctionId, user.getId(), e.getMessage());
            throw e;
        }

        log.info("[RedissonLock] 입찰 처리 완료 및 락 해제 - auctionId={}, userId={}, bidAmount={}",
                auctionId, user.getId(), bidAmount);

        return BidResponse.from(savedBid);
    }
}
