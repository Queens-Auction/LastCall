package org.example.lastcall.domain.bid;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.service.query.AuctionFinder;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.bid.service.query.BidQueryService;
import org.example.lastcall.domain.user.entity.User;
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
class BidQueryServiceTest {
	@Mock
	private AuctionFinder auctionFinder;

	@Mock
	private BidRepository bidRepository;

	@InjectMocks
	private BidQueryService bidQueryService;

	@Test
	@DisplayName("경매별 전체 입찰 내역 조회 성공 (최신순)")
	void getAllBids_경매별_전체_입찰_내역_조회에_성공한다() {
		// given
		Long auctionId = 10L;
		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

		Auction auction = mock(Auction.class);
		given(auctionFinder.findById(auctionId)).willReturn(auction);

		User user1 = mock(User.class);
		given(user1.getNickname()).willReturn("강아지");
		Bid bid1 = Bid.of(10000L, auction, user1);
		ReflectionTestUtils.setField(bid1, "createdAt", LocalDateTime.now().minusMinutes(1));

		User user2 = mock(User.class);
		given(user2.getNickname()).willReturn("고양이");
		Bid bid2 = Bid.of(11000L, auction, user2);
		ReflectionTestUtils.setField(bid2, "createdAt", LocalDateTime.now());

		List<Bid> bidList = Arrays.asList(bid1, bid2);
		Page<Bid> bidPage = new PageImpl<>(bidList, pageable, bidList.size());

		given(bidRepository.findAllByAuction(auction, pageable)).willReturn(bidPage);

		PageResponse<BidGetAllResponse> bids = bidQueryService.getAllBids(auctionId, pageable);

		assertThat(bids).isNotNull();
		assertThat(bids.getContent()).hasSize(2); // 내용물이 2개인지 확인
		assertThat(bids.getNumber()).isEqualTo(0); // 현재 페이지 번호 확인
		assertThat(bids.getTotalElements()).isEqualTo(2); // 전체 요소 개수 확인

		BidGetAllResponse firstBidDto = bids.getContent().get(0);
		assertThat(firstBidDto.getBidAmount()).isEqualTo(10000L);
		assertThat(firstBidDto.getNickname()).isEqualTo("강아지");

		BidGetAllResponse secondBidDto = bids.getContent().get(1);
		assertThat(secondBidDto.getBidAmount()).isEqualTo(11000L);
		assertThat(secondBidDto.getNickname()).isEqualTo("고양이");

		verify(bidRepository, times(1)).findAllByAuction(eq(auction), eq(pageable));
	}

	@Test
	@DisplayName("존재하지 않는 경매 ID로 입찰 조회 시 예외 발생")
	void getAllBids_존재하지_않는_경매라면_예외가_발생한다() {
		Long auctionId = 100L;
		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

		given(auctionFinder.findById(auctionId))
			.willThrow(new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

		assertThatThrownBy(() -> bidQueryService.getAllBids(auctionId, pageable))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(AuctionErrorCode.AUCTION_NOT_FOUND.getMessage());

		verify(auctionFinder, times(1)).findById(auctionId);
		verify(bidRepository, never()).findAllByAuction(any(), any());
	}

	@Test
	@DisplayName("입찰 존재 여부 조회 성공")
	void existsByAuctionIdAndUserId_입찰_존재_여부_조회에_성공한다() {
		Long auctionId = 1L;
		Long userId = 2L;

		given(bidRepository.existsByAuctionIdAndUserId(auctionId, userId))
			.willReturn(true);

		boolean result = bidQueryService.existsByAuctionIdAndUserId(auctionId, userId);

		assertThat(result).isTrue();
		verify(bidRepository, times(1)).existsByAuctionIdAndUserId(auctionId, userId);
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 성공")
	void getCurrentBidAmount_현재_최고_입찰가_조회에_성공한다() {
		Long auctionId = 1L;
		Auction auction = mock(Auction.class);

		given(auctionFinder.findById(auctionId)).willReturn(auction);
		given(bidRepository.findMaxBidAmountByAuction(auction)).willReturn(Optional.of(20000L));

		Long result = bidQueryService.getCurrentBidAmount(auctionId);

		assertThat(result).isEqualTo(20000L);
		verify(auctionFinder, times(1)).findById(auctionId);
		verify(bidRepository, times(1)).findMaxBidAmountByAuction(auction);
	}

	@Test
	@DisplayName("입찰 ID로 입찰 조회 성공")
	void findById_해당_입찰_ID의_입찰_조회에_성공한다() {
		Long bidId = 1L;

		Auction auction = mock(Auction.class);
		User user = mock(User.class);

		Bid bid = Bid.of(10000L, auction, user);

		given(bidRepository.findById(bidId)).willReturn(Optional.of(bid));

		Bid result = bidQueryService.findById(bidId);

		assertThat(result).isNotNull();
		assertThat(result.getBidAmount()).isEqualTo(10000L);
		verify(bidRepository, times(1)).findById(bidId);
	}

	@Test
	@DisplayName("존재하지 않는 입찰 ID로 조회 시 예외 발생")
	void findById_해당_입찰_ID가_존재하지_않으면_예외가_발생한다() {
		Long bidId = 100L;
		given(bidRepository.findById(bidId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> bidQueryService.findById(bidId))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining(BidErrorCode.BID_NOT_FOUND.getMessage());

		verify(bidRepository, times(1)).findById(bidId);
	}
}