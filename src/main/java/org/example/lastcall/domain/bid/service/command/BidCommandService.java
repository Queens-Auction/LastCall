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

    // 입찰 등록
    public BidResponse createBid(Long auctionId, AuthUser authUser) {
        // 입찰이 가능한 경매인지 확인하고, 경매를 받아옴
        Auction auction = auctionQueryServiceApi.getBiddableAuction(auctionId);

        if (auction.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(BidErrorCode.SELLER_CANNOT_BID);
        }

        User user = userServiceApi.findById(authUser.userId());

        // 추가 -> 해당 유저가 이미 경매에 입찰했는지 확인
        boolean alreadyParticipated = bidRepository.existsByAuctionIdAndUserId(
                auction.getId(),
                user.getId()
        );

        // orElse: Optional 객체가 비어있을 경우, 해당 값(시작 값)을 반환함
        Long currentMaxBid = bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());

        Long bidAmount = currentMaxBid + auction.getBidStep();

        // 경매에 참여할만큼 포인트가 충분한 지 검증함
        pointQueryServiceApi.validateSufficientPoints(user.getId(), bidAmount);

        Bid bid = new Bid(bidAmount, auction, user);

        Bid savedBid = bidRepository.save(bid);

        // 추가 -> 중복 입찰 아닌 경우에만 참여자 수 증가
        if (!alreadyParticipated) {
            auction.incrementParticipantCount();
        }

        // 현재 입찰가 갱신
        auction.updateCurrentBid(bidAmount);

        pointCommandServiceApi.updateDepositPoint(
                auction.getId(),
                savedBid.getId(),
                bidAmount,
                user.getId());

        return BidResponse.from(savedBid);
    }
}
