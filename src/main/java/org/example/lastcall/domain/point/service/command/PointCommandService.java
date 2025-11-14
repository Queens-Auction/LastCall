package org.example.lastcall.domain.point.service.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.lock.DistributedLock;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.query.AuctionFinder;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.point.dto.request.PointCreateRequest;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PointCommandService implements PointCommandServiceApi {
	private final PointRepository pointRepository;
	private final PointLogRepository pointLogRepository;
	private final UserQueryServiceApi userQueryServiceApi;
	private final BidQueryServiceApi bidQueryServiceApi;
	private final AuctionFinder auctionFinder;
	private final AuctionRepository auctionRepository;
	private final PointTransactionService pointTransactionService;

	@CacheEvict(value = "userPoints", key = "#authUser.userId()")
	@DistributedLock(key = "'user:' + #authUser.userId()")
	public PointResponse createPoint(AuthUser authUser, @Valid PointCreateRequest request) {
		log.debug("락 획득 후 작업 실행: 포인트 충전 요청 - userId: {}, incomePoint: {}", authUser.userId(), request.getIncomePoint());

		User user = userQueryServiceApi.findById(authUser.userId());

		Point currentPoint = pointRepository.findByUser(user).orElse(null);

		Long incomePoint = request.getIncomePoint();

		PointLogType type = request.getType();

		if (currentPoint == null) {
			currentPoint = Point.of(user, type, incomePoint);
			currentPoint = pointRepository.save(currentPoint);
		} else {
			currentPoint.updateAvailablePoint(incomePoint);
		}

		log.debug("락을 점유한 작업 종료: 포인트 충전 완료 - userId: {}, currentPoint: {}", authUser.userId(), currentPoint.getAvailablePoint());

		PointLog log = pointLogRepository.save(
			PointLog.of(
				currentPoint,
				user.getId(),
				PointLogType.EARN,
				PointLogType.EARN.getDescription(),
				incomePoint));

		return new PointResponse(
			user.getId(),
			currentPoint.getId(),
			currentPoint.getAvailablePoint(),
			currentPoint.getDepositPoint(),
			currentPoint.getSettlementPoint());
	}

	@Override
	@CacheEvict(value = "userPoints", key = "#userId")
	@DistributedLock(key = "'user:' + #userId")
	public void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId) {
		log.debug("락 획득 후 작업 실행: 입찰 포인트 예치 - userId: {}, auctionId: {}, bidAmount: {}", userId, auctionId, bidAmount);

		Point point = pointRepository.findByUserId(userId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND));

		boolean alreadyProcessed = pointLogRepository.existsByBidIdAndTypeIn(bidId, List.of(PointLogType.DEPOSIT, PointLogType.ADDITIONAL_DEPOSIT));

		if (alreadyProcessed) {
			throw new BusinessException(PointErrorCode.ALREADY_PROCESSED_DEPOSIT);
		}

		Optional<Bid> existingBid = bidQueryServiceApi.findLastBidExceptBidId(auctionId, userId, bidId);

		if (existingBid.isPresent()) {
			Bid previousBid = existingBid.get();
			Long previousBidAmount = previousBid.getBidAmount();

			if (bidAmount > previousBidAmount) {
				Long difference = bidAmount - previousBidAmount;

				if (point.getAvailablePoint() < difference) {
					throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
				}

				point.decreaseAvailablePoint(difference);

				point.increaseDepositPoint(difference);

				PointLog log = PointLog.of(
					point,
					userId,
					PointLogType.ADDITIONAL_DEPOSIT,
					"입찰 금액 증가로 인한 추가 예치 처리",
					difference,
					auctionId,
					bidId);

				// 포인트 로그에 저장
				pointLogRepository.save(log);
			}
		} else {
			if (point.getAvailablePoint() < bidAmount) {
				throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
			}

			point.updateDepositPoint(bidAmount);

			PointLog log = PointLog.of(
				point,
				userId,
				PointLogType.DEPOSIT,
				"입찰금 예치 처리",
				bidAmount,
				auctionId,
				bidId
			);

			pointLogRepository.save(log);
		}
		log.debug("락을 점유한 작업 종료: 포인트 예치 완료 - userId: {}, availablePoint: {}, depositPoint: {}", userId, point.getAvailablePoint(), point.getDepositPoint());
	}

	@Override
	public void depositToSettlement(Long userId, Long auctionId, Long amount) { // TODO <-- 여기서 userId와 amount 는 사용하지 않는 parameter 임, userId를 빼는 순간 @CacheEvict 를 사용할 수 없기 때문에 메서드 내에서 CacheManager 를 이용해서 처리해야 함.
		Auction auction = auctionFinder.findById(auctionId);

		Bid highestBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElseThrow(
			() -> new BusinessException(BidErrorCode.BID_NOT_FOUND));

		pointTransactionService.depositToSettlementToUser(auction, highestBid.getUser().getId(), highestBid.getBidAmount());
	}

	@Override
	public void depositToAvailablePoint(Long userId, Long auctionId, Long amount) { // TODO <-- userId 필요 없음, amount 도 필요 없음 사용안함
		Auction auction = auctionRepository.findById(auctionId).orElseThrow(
			() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

		Bid highestBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElseThrow(
			() -> new BusinessException(BidErrorCode.BID_NOT_FOUND));

		Long winnerUserId = highestBid.getUser().getId();

		List<Bid> allBids = bidQueryServiceApi.findAllByAuctionId(auction.getId());

		// Set<Bid> filteredBids = allBids.stream().filter(e -> !e.getUser().getId().equals(winnerUserId))
		// 	.collect(Collectors.toSet());
		//
		// filteredBids.forEach(e -> {
		// 	// 메서드 분리함
		// 	pointLockService.depositToAvailablePointToUser(auction, e.getUser().getId(), winnerUserId, e);
		// });

		// 중복 입찰자는 최고 입찰가만 반영 (추가)
		Map<Long, Bid> latestBidsByUser = new HashMap<>();

		for (Bid bid : allBids) {
			Long bidderId = bid.getUser().getId();
			Bid currentHighestBid = latestBidsByUser.get(bidderId);

			if ((currentHighestBid == null || bid.getBidAmount() > currentHighestBid.getBidAmount())
				&& !bidderId.equals(winnerUserId)) {
				latestBidsByUser.put(bidderId, bid);
			}
		}

		// 낙찰 실패자만 처리
		for (Map.Entry<Long, Bid> entry : latestBidsByUser.entrySet()) {
			Long loserId = entry.getKey();
			Bid finalBid = entry.getValue();

			System.out.println("loserId : " + loserId + " / winnerUserId : " + winnerUserId);

			// 메서드 분리함
			pointTransactionService.depositToAvailablePointToUser(auction, loserId, winnerUserId, finalBid);
		}
	}
}