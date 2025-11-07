package org.example.lastcall.domain.point.service.query;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PointQueryService implements PointQueryServiceApi {
	private final PointRepository pointRepository;
	private final UserServiceApi userServiceApi;

	// 유저 포인트 조회
	// @Cacheable: 캐시 정보를 메모리 상에 저장하거나 조회해오는 기능 수행
	@Cacheable(value = "userPoints", key = "#authUser.userId()")
	public PointResponse getUserPoint(AuthUser authUser) {

		User user = userServiceApi.findById(authUser.userId());

		Point point = pointRepository.findByUserId(authUser.userId()).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
		);

		return new PointResponse(
			user.getId(),
			point.getId(),
			point.getAvailablePoint(),
			point.getDepositPoint(),
			point.getSettlementPoint()
		);
	}

	// 현재 보유 포인트로 입찰이 가능한지 확인하는 메서드
	@Override
	public void validateSufficientPoints(Long userId, Long requiredAmount) {
		Point point = pointRepository.findByUserId(userId).orElseThrow(
			() -> new BusinessException(PointErrorCode.POINT_ACCOUNT_NOT_FOUND)
		);

		if (point.getAvailablePoint() < requiredAmount) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
		}
	}
}