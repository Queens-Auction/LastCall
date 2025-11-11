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

/**
 * @TODO 적절한 명칭 변경 필요
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PointLockService {
	private final PointRepository pointRepository;
	private final PointLogRepository pointLogRepository;
	private final CacheManager cacheManager;

	// userId로 lock을 걸어야하기 때문에 메서드 분리함
	@DistributedLock(key = "'user:' + #winnerUserId")
	public void depositToSettlementToUser(Auction auction, Long winnerUserId, Long winnerBidAmount) {
		log.debug("락 획득 후 작업 실행: 예치 포인트 정산 - userId: {}, auctionId: {}, amount: {}", winnerUserId, auction.getId(),
			winnerBidAmount);

		// auction에 해당하는 정산 내역이 있는지 확인함
		boolean alreadyProcessed = pointLogRepository.existsByAuctionIdAndTypeAndUserId(auction.getId(),
			PointLogType.SETTLEMENT, winnerUserId);
		if (alreadyProcessed) {
			throw new BusinessException(PointErrorCode.ALREADY_PROCESSED_SETTLEMENT);
		}

		// TODO 아래코드 추가하면 위에 @CacheEvict 필요 없음
		// Objects.requireNonNull(cacheManager.getCache("userPoints")).evictIfPresent(winnerUserId);
		Cache cache = cacheManager.getCache("userPoints");
		if (cache != null) {
			cache.evict(winnerUserId);
		}

		// 낙찰자의 포인트 계좌 조회
		Point point = pointRepository.findByUserId(winnerUserId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_ACCOUNT_NOT_FOUND)
		);

		// 예치 포인트 -> 정산 포인트로 이동
		point.depositToSettlement(winnerBidAmount);

		// 변경사항 저장
		pointRepository.save(point);

		// 포인트 로그에 기록 / 저장
		pointLogRepository.save(PointLog.create(
			point,
			point.getUser().getId(),
			PointLogType.SETTLEMENT,
			"입찰 확정으로 인한 정산 포인트 이동",
			winnerBidAmount,
			auction.getId()
		));

		log.debug("락을 점유한 작업 종료: 포인트 정산 완료 - userId: {}, depositPoint: {}, settlementPoint: {}", winnerUserId,
			point.getDepositPoint(), point.getSettlementPoint());
	}

	// userId로 lock을 걸어야하기 때문에 메서드 분리함
	@DistributedLock(key = "'user:' + #loserId")
	public void depositToAvailablePointToUser(Auction auction, Long loserId, Long winnerUserId, Bid finalBid) {
		log.debug("락 획득 후 작업 실행: 낙찰 실패자 환불 시작 - auctionId: {}, userId: {}", auction.getId(), loserId);

		// 낙찰자면 스킵
		if (loserId.equals(winnerUserId)) {
			return;
		}

		boolean alreadyProcessed = pointLogRepository.existsByAuctionIdAndTypeAndUserId(auction.getId(),
			PointLogType.DEPOSIT_TO_AVAILABLE, loserId);
		if (alreadyProcessed) {
			throw new BusinessException(PointErrorCode.ALREADY_REFUNDED_DEPOSIT);
		}

		// TODO 아래코드 추가하면 위에 @CacheEvict 가 필요 없습니다.
		// Objects.requireNonNull(cacheManager.getCache("userPoints")).evictIfPresent(winnerUserId);
		Cache cache = cacheManager.getCache("userPoints");
		if (cache != null) {
			cache.evict(loserId);
		}

		Long bidAmount = finalBid.getBidAmount();

		// 포인트 계좌 조회
		Point point = pointRepository.findByUserId(loserId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
		);

		// 이동 가능 여부 조회
		if (!point.canMoveDepositToAvailable(bidAmount)) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_DEPOSIT_POINT);
		}

		// 포인트 이동 (예치 -> 가용)
		point.moveDepositToAvailable(bidAmount);
		pointRepository.save(point);

		// 유저 참조 가져오기
		// TODO user 조회하는 쿼리를 줄여서 성능을 높이겠다.
		// User user = userServiceApi.getReferenceById(loserId);

		// 포인트 로그에 기록
		pointLogRepository.save(PointLog.create(
			point,      // 추가
			loserId,
			PointLogType.DEPOSIT_TO_AVAILABLE,
			PointLogType.DEPOSIT_TO_AVAILABLE.getDescription(),
			bidAmount,
			auction.getId()
		));
	}
}
