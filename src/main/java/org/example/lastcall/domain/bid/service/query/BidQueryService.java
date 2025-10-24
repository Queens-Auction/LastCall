package org.example.lastcall.domain.bid.service.query;

import java.util.List;
import java.util.Optional;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BidQueryService implements BidQueryServiceApi {
	private final BidRepository bidRepository;
	private final AuctionServiceApi auctionServiceApi;

	// 경매별 전체 입찰 내역 조회
	@Transactional(readOnly = true)
	public PageResponse<BidGetAllResponse> getAllBids(Long auctionId, Pageable pageable) {
		Auction auction = auctionServiceApi.findById(auctionId);

		Page<Bid> bidPage = bidRepository.findAllByAuction(auction, pageable);

		return PageResponse.of(bidPage.map(BidGetAllResponse::from));
	}

	@Override
	public boolean existsByAuctionIdAndUserId(Long auctionId, Long userId) {
		// if (auctionId == null || userId == null) {
		// 	throw new BusinessException(BidErrorCode.INVALID_BID_CHECK_REQUEST);
		// }
		return bidRepository.existsByAuctionIdAndUserId(auctionId, userId);
	}

	@Override
	public Long getCurrentBidAmount(Long auctionId) {
		Auction auction = auctionServiceApi.findById(auctionId);

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
	public Optional<Bid> findExistingBid(Long auctionId, Long userId, Long bidId) {
		return bidRepository.findTopByAuctionIdAndUserIdAndIdNotOrderByBidAmountDesc(auctionId, userId, bidId);
	}

	@Override
	public Bid getBid(Long bidId) {
		return bidRepository.findById(bidId).orElseThrow(
			() -> new BusinessException(BidErrorCode.BID_NOT_FOUND)
		);
	}

	// 내가 참여한 경매 목록 조회
	@Override
	public List<Long> getParticipatedAuctionIds(Long userId) {
		return bidRepository.findDistinctAuctionsByUserId(userId);
	}

	// 특정 유저가 특정 경매에서 최고 입찰자인지 여부 조회
	@Override
	public boolean isUserLeading(Long auctionId, Long userId) {
		Long currentBid = getCurrentBidAmount(auctionId);
		Long myBidAmount = getMyBidAmount(auctionId, userId);
		return myBidAmount != null && myBidAmount.equals(currentBid);
	}

	// 특정 유저가 해당 경매에서 입찰한 금액 중 가장 높은 금액 조회
	@Override
	public Long getMyBidAmount(Long auctionId, Long userId) {
		return bidRepository.findTopByAuctionIdAndUserIdOrderByBidAmountDesc(auctionId, userId)
			.map(Bid::getBidAmount)
			.orElse(null);
	}
}
