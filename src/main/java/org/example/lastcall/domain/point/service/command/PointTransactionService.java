package org.example.lastcall.domain.point.service.command;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.lock.DistributedLock;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PointTransactionService {
	private final PointRepository pointRepository;
	private final PointLogRepository pointLogRepository;
	private final CacheManager cacheManager;

	@DistributedLock(key = "'user:' + #winnerUserId")
	public void depositToSettlementToUser(Auction auction, Long winnerUserId, Long winnerBidAmount) {
		log.debug("락 획득 후 작업 실행: 예치 포인트 정산 - userId: {}, auctionId: {}, amount: {}", winnerUserId, auction.getId(), winnerBidAmount);

		boolean alreadyProcessed = pointLogRepository.existsByAuctionIdAndTypeAndUserId(auction.getId(), PointLogType.SETTLEMENT, winnerUserId);

		if (alreadyProcessed) {
			throw new BusinessException(PointErrorCode.ALREADY_PROCESSED_SETTLEMENT);
		}

		Cache cache = cacheManager.getCache("userPoints");

		if (cache != null) {
			cache.evict(winnerUserId);
		}

		Point point = pointRepository.findByUserId(winnerUserId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_ACCOUNT_NOT_FOUND));

		point.depositToSettlement(winnerBidAmount);

		pointRepository.save(point);

		pointLogRepository.save(PointLog.of(
			point,
			point.getUser().getId(),
			PointLogType.SETTLEMENT,
			"입찰 확정으로 인한 정산 포인트 이동",
			winnerBidAmount,
			auction.getId()));

		log.debug("락을 점유한 작업 종료: 포인트 정산 완료 - userId: {}, depositPoint: {}, settlementPoint: {}", winnerUserId, point.getDepositPoint(), point.getSettlementPoint());
	}

	@DistributedLock(key = "'user:' + #loserId")
	public void depositToAvailablePointToUser(Auction auction, Long loserId, Long winnerUserId, Bid finalBid) {
		log.debug("락 획득 후 작업 실행: 낙찰 실패자 환불 시작 - auctionId: {}, userId: {}", auction.getId(), loserId);

		if (loserId.equals(winnerUserId)) {
			return;
		}

		boolean alreadyProcessed = pointLogRepository.existsByAuctionIdAndTypeAndUserId(auction.getId(), PointLogType.DEPOSIT_TO_AVAILABLE, loserId);

		if (alreadyProcessed) {
			throw new BusinessException(PointErrorCode.ALREADY_REFUNDED_DEPOSIT);
		}

		Cache cache = cacheManager.getCache("userPoints");

		if (cache != null) {
			cache.evict(loserId);
		}

		Long bidAmount = finalBid.getBidAmount();

		Point point = pointRepository.findByUserId(loserId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND));

		if (!point.canMoveDepositToAvailable(bidAmount)) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_DEPOSIT_POINT);
		}

		point.moveDepositToAvailable(bidAmount);
		pointRepository.save(point);

		pointLogRepository.save(PointLog.of(
			point,
			loserId,
			PointLogType.DEPOSIT_TO_AVAILABLE,
			PointLogType.DEPOSIT_TO_AVAILABLE.getDescription(),
			bidAmount,
			auction.getId()));
	}
}
