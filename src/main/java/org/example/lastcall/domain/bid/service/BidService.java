package org.example.lastcall.domain.bid.service;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.service.PointServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BidService implements BidServiceApi {
	private final BidRepository bidRepository;
	private final AuctionServiceApi auctionServiceApi;
	private final UserRepository userRepository;
	private final PointServiceApi pointServiceApi;

	// 입찰 등록
	// userId는 인증/인가 구현 완료되면 이후 수정하기 (@Auth AuthUser authUser)
	public BidResponse createBid(Long auctionId, Long userId) {
		// 입찰이 가능한 경매인지 확인하고, 경매를 받아옴. findById?
		Auction auction = auctionServiceApi.getBiddableAuction(auctionId);

		User user = userRepository.findById(userId)    // 만들어주시면 수정하기!
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

		// orElse: Optional 객체가 비어있을 경우, 해당 값(시작 값)을 반환해라.
		Long currentMaxBid = bidRepository.findMaxBidAmountByAuction(auction).orElse(auction.getStartingBid());

		Long bidAmount = currentMaxBid + auction.getBidStep();

		// 경매에 참여할만큼 포인트가 충분한 지 검증
		pointServiceApi.validateSufficientPoints(userId, bidAmount);

		Bid bid = new Bid(bidAmount, auction, user);

		Bid savedBid = bidRepository.save(bid);

		pointServiceApi.updateDepositPoint(auction.getId(), savedBid.getId(), bidAmount, userId);

		return BidResponse.from(savedBid);
	}
}
