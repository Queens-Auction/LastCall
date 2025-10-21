package org.example.lastcall.domain.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.service.PointServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {
	@Mock    // 테스트 대상 외의 의존 객체
	private AuctionServiceApi auctionServiceApi;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PointServiceApi pointServiceApi;

	@Mock
	private BidRepository bidRepository;

	@InjectMocks    // 테스트 대상 객체
	private BidService bidService;

	@Test
	@DisplayName("기존 입찰이 있을 경우, 입찰 성공")
	void createBid_기존_입찰이_있을_때_입찰_등록에_성공한다() {
		// given
		Long auctionId = 10L;
		Long userId = 1L;
		Long bidId = 2L;
		Long currentMaxBid = 500L; // 현재 최고 입찰가
		Long bidStep = 10L;
		Long bidAmount = currentMaxBid + bidStep;

		Auction auction = mock(Auction.class);    //  가짜 객체 생성

		User user = mock(User.class);

		// 원하는 값 반환하도록 설정
		given(auction.getBidStep()).willReturn(bidStep);
		given(auction.getId()).willReturn(auctionId);

		given(auctionServiceApi.getBiddableAuction(auctionId)).willReturn(auction);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.of(currentMaxBid));
		willDoNothing().given(pointServiceApi)
			.validateSufficientPoints(userId, bidAmount);    // 해당 메서드 호출 시, (성공한 것처럼) 아무런 일도 하지 않고 그냥 넘어감

		Bid savedBid = new Bid(bidAmount, auction, user);
		ReflectionTestUtils.setField(savedBid, "id", bidId);

		given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

		// when
		BidResponse bid = bidService.createBid(auctionId, userId);

		// then
		assertThat(bid).isNotNull();
		assertThat(bid.getBidAmount()).isEqualTo(bidAmount);

		verify(pointServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(bidAmount));
		verify(bidRepository, times(1)).save(any(Bid.class));
		verify(pointServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(bidAmount), eq(userId));
	}

	@Test
	@DisplayName("기존 입찰이 없을 경우, 입찰 성공")
	void createBid_기존_입찰이_없을_때_입찰_등록에_성공한다() {
		// given
		Long auctionId = 10L;
		Long userId = 1L;
		Long bidId = 2L;
		Long startingBid = 500L; // 시작가
		Long bidStep = 10L;
		Long bidAmount = startingBid + bidStep;

		Auction auction = mock(Auction.class);    //  가짜 객체 생성

		User user = mock(User.class);

		// 원하는 값 반환하도록 설정
		given(auction.getBidStep()).willReturn(bidStep);
		given(auction.getStartingBid()).willReturn(startingBid);
		given(auction.getId()).willReturn(auctionId);

		given(auctionServiceApi.getBiddableAuction(auctionId)).willReturn(auction);
		given(userRepository.findById(userId)).willReturn(Optional.of(user));

		given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.empty());
		willDoNothing().given(pointServiceApi).validateSufficientPoints(userId, bidAmount);

		Bid savedBid = new Bid(bidAmount, auction, user);
		ReflectionTestUtils.setField(savedBid, "id", bidId);

		given(bidRepository.save(any(Bid.class))).willReturn(savedBid);

		// when
		BidResponse bid = bidService.createBid(auctionId, userId);

		// then
		assertThat(bid).isNotNull();
		assertThat(bid.getBidAmount()).isEqualTo(bidAmount);

		verify(pointServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(bidAmount));
		verify(bidRepository, times(1)).save(any(Bid.class));
		verify(pointServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(bidAmount), eq(userId));
	}
}