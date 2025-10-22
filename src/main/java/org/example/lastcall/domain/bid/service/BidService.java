package org.example.lastcall.domain.bid.service;

import java.util.Optional;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.service.PointServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BidService implements BidServiceApi {
	private final BidRepository bidRepository;
	private final AuctionServiceApi auctionServiceApi;
	private final UserServiceApi userServiceApi;
	private final PointServiceApi pointServiceApi;

	// 입찰 등록
	public BidResponse createBid(Long auctionId, AuthUser authUser) {
		// 입찰이 가능한 경매인지 확인하고, 경매를 받아옴
		Auction auction = auctionServiceApi.getBiddableAuction(auctionId);

		if (auction.getUser().getId().equals(authUser.userId())) {
			throw new BusinessException(BidErrorCode.SELLER_CANNOT_BID);
		}

		User user = userServiceApi.findById(authUser.userId());

		// orElse: Optional 객체가 비어있을 경우, 해당 값(시작 값)을 반환함
		Long currentMaxBid = bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());

		Long bidAmount = currentMaxBid + auction.getBidStep();

		// 경매에 참여할만큼 포인트가 충분한 지 검증함
		pointServiceApi.validateSufficientPoints(user.getId(), bidAmount);

		Bid bid = new Bid(bidAmount, auction, user);

		Bid savedBid = bidRepository.save(bid);

		pointServiceApi.updateDepositPoint(auction.getId(), savedBid.getId(), bidAmount, user.getId());

		return BidResponse.from(savedBid);
	}

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
}
