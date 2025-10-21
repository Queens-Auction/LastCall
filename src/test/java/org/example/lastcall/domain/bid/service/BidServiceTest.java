package org.example.lastcall.domain.bid.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

	@Test
	@DisplayName("경매별 전체 입찰 내역 조회 성공 (최신순)")
	void getAllBids_경매별_전체_입찰_내역_조회에_성공한다() {
		// given
		Long auctionId = 10L;
		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

		Auction auction = mock(Auction.class);    //  가짜 객체 생성

		given(auctionServiceApi.findById(auctionId)).willReturn(auction);

		// Mock 데이터 준비 (Page<Bid> 객체)
		User user1 = mock(User.class);
		given(user1.getNickname()).willReturn("귀염둥이");
		Bid bid1 = new Bid(11000L, auction, user1);
		ReflectionTestUtils.setField(bid1, "createdAt", LocalDateTime.now()); // 시간 설정

		User user2 = mock(User.class);
		given(user2.getNickname()).willReturn("냥냥");
		Bid bid2 = new Bid(10000L, auction, user2);
		ReflectionTestUtils.setField(bid2, "createdAt", LocalDateTime.now().minusMinutes(1)); // 이전 시간

		List<Bid> bidList = Arrays.asList(bid1, bid2); // Mock Bid 리스트
		Page<Bid> bidPage = new PageImpl<>(bidList, pageable, bidList.size()); // Mock Page 객체 생성

		// Repository가 Mock Page 객체를 반환하도록 설정
		given(bidRepository.findAllByAuction(auction, pageable)).willReturn(bidPage);

		// when
		PageResponse<BidGetAllResponse> bids = bidService.getAllBids(auctionId, pageable);

		// then
		assertThat(bids).isNotNull();
		assertThat(bids.getContent()).hasSize(2); // 내용물이 2개인지 확인
		assertThat(bids.getNumber()).isEqualTo(0); // 현재 페이지 번호 확인
		assertThat(bids.getTotalElements()).isEqualTo(2); // 전체 요소 개수 확인

		// 첫 번째 요소(최신)의 내용 검증 (DTO 변환 확인)
		BidGetAllResponse firstBidDto = bids.getContent().get(0);
		assertThat(firstBidDto.getBidAmount()).isEqualTo(11000L);
		assertThat(firstBidDto.getNickname()).isEqualTo("귀염둥이");

		// Repository 메서드가 정확히 1번 호출되었는지 검증
		verify(bidRepository, times(1)).findAllByAuction(eq(auction), eq(pageable));
	}
}