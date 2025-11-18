package org.example.lastcall.domain.bid;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.bid.service.command.BidCommandService;
import org.example.lastcall.domain.point.service.command.PointCommandServiceApi;
import org.example.lastcall.domain.point.service.query.PointQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BidCommandServiceTest {
    @Mock
    PointQueryServiceApi pointQueryServiceApi;
    @Mock
    private AuctionQueryServiceApi auctionQueryServiceApi;
    @Mock
    private UserQueryServiceApi userQueryServiceApi;
    @Mock
    private PointCommandServiceApi pointCommandServiceApi;
    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private BidCommandService bidCommandService;

    private AuthUser createAuthUser(Long userId) {
        return new AuthUser(userId, "public" + userId, "USER");
    }

    @Test
    @DisplayName("기존 입찰이 있을 경우, 입찰 성공")
    void createBid_기존_입찰_존재_시_입찰_등록에_성공한다() {
        Long auctionId = 10L;
        Long sellerId = 99L;
        Long userId = 1L;
        Long bidId = 2L;
        Long currentMaxBid = 500L;
        Long bidStep = 10L;
        Long expectedNextBidAmount = currentMaxBid + bidStep;
        AuthUser authUser = createAuthUser(userId);

        Auction auction = mock(Auction.class);
        User seller = mock(User.class);
        User user = mock(User.class);

        given(seller.getId()).willReturn(sellerId);
        given(user.getId()).willReturn(userId);

        given(auction.getUser()).willReturn(seller);
        given(auction.getBidStep()).willReturn(bidStep);
        given(auction.getId()).willReturn(auctionId);

        given(auctionQueryServiceApi.findBiddableAuction(auctionId)).willReturn(auction);
        given(userQueryServiceApi.findById(userId)).willReturn(user);

        given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.of(currentMaxBid));
        willDoNothing().given(pointQueryServiceApi).validateSufficientPoints(userId, expectedNextBidAmount);

        Bid savedBid = Bid.of(expectedNextBidAmount, auction, user);
        ReflectionTestUtils.setField(savedBid, "id", bidId);
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        BidResponse bid = bidCommandService.createBid(auctionId, authUser, expectedNextBidAmount);

        assertThat(bid).isNotNull();
        assertThat(bid.getBidAmount()).isEqualTo(expectedNextBidAmount);

        verify(pointQueryServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(expectedNextBidAmount));
        verify(bidRepository, times(1)).save(any(Bid.class));
        verify(pointCommandServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(expectedNextBidAmount), eq(userId));
    }

    @Test
    @DisplayName("기존 입찰이 없을 경우, 입찰 성공")
    void createBid_기존_입찰이_없을_때_입찰_등록에_성공한다() {
        Long auctionId = 10L;
        Long sellerId = 20L;
        Long userId = 1L;
        Long bidId = 1L;
        Long startingBid = 500L;
        Long bidStep = 10L;
        Long expectedNextBidAmount = startingBid + bidStep;
        AuthUser authUser = createAuthUser(userId);

        Auction auction = mock(Auction.class);
        User seller = mock(User.class);
        User user = mock(User.class);

        given(seller.getId()).willReturn(sellerId);
        given(user.getId()).willReturn(userId);

        given(auction.getUser()).willReturn(seller);
        given(auction.getBidStep()).willReturn(bidStep);
        given(auction.getStartingBid()).willReturn(startingBid);
        given(auction.getId()).willReturn(auctionId);

        given(auctionQueryServiceApi.findBiddableAuction(auctionId)).willReturn(auction);
        given(userQueryServiceApi.findById(userId)).willReturn(user);

        given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.empty());
        willDoNothing().given(pointQueryServiceApi).validateSufficientPoints(userId, expectedNextBidAmount);

        Bid savedBid = Bid.of(expectedNextBidAmount, auction, user);
        ReflectionTestUtils.setField(savedBid, "id", bidId);
        given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

        BidResponse bid = bidCommandService.createBid(auctionId, authUser, expectedNextBidAmount);

        assertThat(bid).isNotNull();
        assertThat(bid.getBidAmount()).isEqualTo(expectedNextBidAmount);

        verify(pointQueryServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(expectedNextBidAmount));
        verify(bidRepository, times(1)).save(any(Bid.class));
        verify(pointCommandServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(expectedNextBidAmount), eq(userId));
    }

    @Test
    @DisplayName("판매자는 본인 경매에 입찰할 수 없으며 예외 발생")
    void createBid_판매자가_본인_경매에_입찰_시도_시_예외가_발생한다() {
        Long auctionId = 10L;
        Long userId = 1L;
        AuthUser authUser = createAuthUser(userId);

        Auction auction = mock(Auction.class);
        User seller = mock(User.class);
        given(seller.getId()).willReturn(userId);
        given(auction.getUser()).willReturn(seller);

        given(auctionQueryServiceApi.findBiddableAuction(auctionId)).willReturn(auction);

        assertThatThrownBy(() -> bidCommandService.createBid(auctionId, authUser, 1000L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", BidErrorCode.SELLER_CANNOT_BID);

        verify(userQueryServiceApi, never()).findById(anyLong());
        verify(bidRepository, never()).save(any(Bid.class));
    }
}