// package org.example.lastcall.domain.bid.service;
//
// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.BDDMockito.*;
//
// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;
//
// import org.example.lastcall.common.exception.BusinessException;
// import org.example.lastcall.common.response.PageResponse;
// import org.example.lastcall.domain.auction.entity.Auction;
// import org.example.lastcall.domain.auction.service.AuctionServiceApi;
// import org.example.lastcall.domain.auth.model.AuthUser;
// import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
// import org.example.lastcall.domain.bid.dto.response.BidResponse;
// import org.example.lastcall.domain.bid.entity.Bid;
// import org.example.lastcall.domain.bid.exception.BidErrorCode;
// import org.example.lastcall.domain.bid.repository.BidRepository;
// import org.example.lastcall.domain.bid.service.query.BidQueryService;
// import org.example.lastcall.domain.point.service.PointServiceApi;
// import org.example.lastcall.domain.user.entity.User;
// import org.example.lastcall.domain.user.service.UserServiceApi;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.data.domain.Sort;
// import org.springframework.test.util.ReflectionTestUtils;
//
// @ExtendWith(MockitoExtension.class)
// class BidQueryServiceTest {
// 	@Mock    // 테스트 대상 외의 의존 객체
// 	private AuctionServiceApi auctionServiceApi;
//
// 	@Mock
// 	private UserServiceApi userServiceApi;
//
// 	@Mock
// 	private PointServiceApi pointServiceApi;
//
// 	@Mock
// 	private BidRepository bidRepository;
//
// 	@InjectMocks    // 테스트 대상 객체
// 	private BidQueryService bidQueryService;
//
// 	// AuthUser 레코드 생성 헬퍼 메서드 (publicId와 role 추가)
// 	private AuthUser createAuthUser(Long userId) {
// 		return new AuthUser(userId, "public" + userId, "USER");
// 	}
//
// 	@Test
// 	@DisplayName("기존 입찰이 있을 경우, 입찰 성공")
// 	void createBid_기존_입찰이_있을_때_입찰_등록에_성공한다() {
// 		// given
// 		Long auctionId = 10L;
// 		Long sellerId = 99L;
// 		Long userId = 1L;
// 		Long bidId = 2L;
// 		Long currentMaxBid = 500L; // 현재 최고 입찰가
// 		Long bidStep = 10L;
// 		Long bidAmount = currentMaxBid + bidStep;
// 		AuthUser authUser = createAuthUser(userId); // AuthUser 사용
//
// 		Auction auction = mock(Auction.class);    //  가짜 객체 생성
//
// 		User seller = mock(User.class);
//
// 		User user = mock(User.class);
//
// 		// 원하는 값 반환하도록 설정
// 		given(seller.getId()).willReturn(sellerId);
//
// 		given(auction.getUser()).willReturn(seller);
// 		given(auction.getBidStep()).willReturn(bidStep);
// 		given(auction.getId()).willReturn(auctionId);
//
// 		given(user.getId()).willReturn(userId);
//
// 		// Mock 동작 설정
// 		given(auctionServiceApi.getBiddableAuction(auctionId)).willReturn(auction);
// 		given(userServiceApi.findById(userId)).willReturn(user);
//
// 		given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.of(currentMaxBid));
// 		willDoNothing().given(pointServiceApi).validateSufficientPoints(userId, bidAmount);
//
// 		Bid savedBid = new Bid(bidAmount, auction, user);
// 		ReflectionTestUtils.setField(savedBid, "id", bidId);
// 		given(bidRepository.save(any(Bid.class))).willReturn(savedBid);
//
// 		// when
// 		BidResponse bid = bidQueryService.createBid(auctionId, authUser);
//
// 		// then
// 		assertThat(bid).isNotNull();
// 		assertThat(bid.getBidAmount()).isEqualTo(bidAmount);
//
// 		// verify 검증 (user.getId()가 아닌 userId로 검증해도 무방합니다.)
// 		verify(pointServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(bidAmount));
// 		verify(bidRepository, times(1)).save(any(Bid.class));
// 		verify(pointServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(bidAmount), eq(userId));
// 	}
//
// 	@Test
// 	@DisplayName("기존 입찰이 없을 경우, 입찰 성공")
// 	void createBid_기존_입찰이_없을_때_입찰_등록에_성공한다() {
// 		// given
// 		Long auctionId = 10L;
// 		Long sellerId = 20L;
// 		Long userId = 1L;
// 		Long bidId = 2L;
// 		Long startingBid = 500L; // 시작가
// 		Long bidStep = 10L;
// 		Long bidAmount = startingBid + bidStep;
// 		AuthUser authUser = createAuthUser(userId); // AuthUser 사용
//
// 		Auction auction = mock(Auction.class);    //  가짜 객체 생성
//
// 		User seller = mock(User.class);
//
// 		User user = mock(User.class);
//
// 		// 원하는 값 반환하도록 설정
// 		given(seller.getId()).willReturn(sellerId);
// 		given(auction.getUser()).willReturn(seller);
//
// 		given(auction.getBidStep()).willReturn(bidStep);
// 		given(auction.getStartingBid()).willReturn(startingBid);
// 		given(auction.getId()).willReturn(auctionId);
//
// 		given(user.getId()).willReturn(userId);
//
// 		// Mock 동작 설정
// 		given(auctionServiceApi.getBiddableAuction(auctionId)).willReturn(auction);
// 		// userServiceApi 사용으로 변경
// 		given(userServiceApi.findById(userId)).willReturn(user);
//
// 		given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.empty());
// 		willDoNothing().given(pointServiceApi).validateSufficientPoints(userId, bidAmount);
//
// 		Bid savedBid = new Bid(bidAmount, auction, user);
// 		ReflectionTestUtils.setField(savedBid, "id", bidId);
// 		given(bidRepository.save(any(Bid.class))).willReturn(savedBid);
//
// 		// when
// 		BidResponse bid = bidQueryService.createBid(auctionId, authUser);
//
// 		// then
// 		assertThat(bid).isNotNull();
// 		assertThat(bid.getBidAmount()).isEqualTo(bidAmount);
//
// 		verify(pointServiceApi, times(1)).validateSufficientPoints(eq(userId), eq(bidAmount));
// 		verify(bidRepository, times(1)).save(any(Bid.class));
// 		verify(pointServiceApi, times(1)).updateDepositPoint(eq(auctionId), eq(bidId), eq(bidAmount), eq(userId));
// 	}
//
// 	@Test
// 	@DisplayName("판매자는 본인 경매에 입찰할 수 없으며, 예외가 발생한다.")
// 	void createBid_판매자가_본인_경매에_입찰_시도_시_예외_발생() {
// 		// given
// 		Long auctionId = 10L;
// 		Long userId = 1L; // 판매자 ID와 동일
// 		AuthUser authUser = createAuthUser(userId);
//
// 		// Auction 설정 (경매 판매자와 입찰자가 동일하도록 설정)
// 		Auction auction = mock(Auction.class);
// 		User seller = mock(User.class);
// 		given(seller.getId()).willReturn(userId); // 판매자 ID를 입찰자 ID와 동일하게 설정
// 		given(auction.getUser()).willReturn(seller);
//
// 		// Mock 동작 설정
// 		given(auctionServiceApi.getBiddableAuction(auctionId)).willReturn(auction);
//
// 		// when & then
// 		assertThatThrownBy(() -> bidQueryService.createBid(auctionId, authUser))
// 			.isInstanceOf(BusinessException.class)
// 			.hasFieldOrPropertyWithValue("errorCode", BidErrorCode.SELLER_CANNOT_BID);
//
// 		// userServiceApi.findById() 및 이후 로직이 호출되지 않았는지 검증
// 		verify(userServiceApi, never()).findById(anyLong());
// 		verify(bidRepository, never()).save(any(Bid.class));
// 	}
//
// 	@Test
// 	@DisplayName("경매별 전체 입찰 내역 조회 성공 (최신순)")
// 	void getAllBids_경매별_전체_입찰_내역_조회에_성공한다() {
// 		// given
// 		Long auctionId = 10L;
// 		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
//
// 		Auction auction = mock(Auction.class);    //  가짜 객체 생성
//
// 		given(auctionServiceApi.findById(auctionId)).willReturn(auction);
//
// 		// Mock 데이터 준비 (Page<Bid> 객체)
// 		User user1 = mock(User.class);
// 		given(user1.getNickname()).willReturn("귀염둥이");
// 		Bid bid1 = new Bid(11000L, auction, user1);
// 		ReflectionTestUtils.setField(bid1, "createdAt", LocalDateTime.now()); // 시간 설정
//
// 		User user2 = mock(User.class);
// 		given(user2.getNickname()).willReturn("냥냥");
// 		Bid bid2 = new Bid(10000L, auction, user2);
// 		ReflectionTestUtils.setField(bid2, "createdAt", LocalDateTime.now().minusMinutes(1)); // 이전 시간
//
// 		List<Bid> bidList = Arrays.asList(bid1, bid2); // Mock Bid 리스트
// 		Page<Bid> bidPage = new PageImpl<>(bidList, pageable, bidList.size()); // Mock Page 객체 생성
//
// 		// Repository가 Mock Page 객체를 반환하도록 설정
// 		given(bidRepository.findAllByAuction(auction, pageable)).willReturn(bidPage);
//
// 		// when
// 		PageResponse<BidGetAllResponse> bids = bidQueryService.getAllBids(auctionId, pageable);
//
// 		// then
// 		assertThat(bids).isNotNull();
// 		assertThat(bids.getContent()).hasSize(2); // 내용물이 2개인지 확인
// 		assertThat(bids.getNumber()).isEqualTo(0); // 현재 페이지 번호 확인
// 		assertThat(bids.getTotalElements()).isEqualTo(2); // 전체 요소 개수 확인
//
// 		// 첫 번째 요소(최신)의 내용 검증 (DTO 변환 확인)
// 		BidGetAllResponse firstBidDto = bids.getContent().get(0);
// 		assertThat(firstBidDto.getBidAmount()).isEqualTo(11000L);
// 		assertThat(firstBidDto.getNickname()).isEqualTo("귀염둥이");
//
// 		// Repository 메서드가 정확히 1번 호출되었는지 검증
// 		verify(bidRepository, times(1)).findAllByAuction(eq(auction), eq(pageable));
// 	}
// }