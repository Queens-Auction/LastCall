package org.example.lastcall.domain.point.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.point.dto.CreatePointRequest;
import org.example.lastcall.domain.point.dto.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.entity.PointLogType;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class PointService implements PointServiceApi {

    private final PointRepository pointRepository;
    private final PointLogRepository pointLogRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public PointResponse createPoint(Long userId, @Valid CreatePointRequest request) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User does not exist.")
        );

        Point currentPoint = pointRepository.findByUser(user).orElse(null);

        Long incomePoint = request.getIncomePoint();

        if (currentPoint == null) {
            currentPoint = Point.create(user, incomePoint);
            currentPoint = pointRepository.save(currentPoint);
        } else {
            currentPoint.updateAvailablePoint(incomePoint);
        }

        PointLog log = pointLogRepository.save(PointLog.create(currentPoint, user, PointLogType.EARN, PointLogType.EARN.getDescription(), incomePoint));

        return new PointResponse(
                user.getId(),
                currentPoint.getId(),
                currentPoint.getAvailablePoint(),
                currentPoint.getDepositPoint(),
                currentPoint.getSettlementPoint()
        );
    }

    public PointResponse getUserPoint(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User does not exist.")
        );

        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new IllegalArgumentException("Point record not found for this user.")
        );

        return new PointResponse(
                user.getId(),
                point.getId(),
                point.getAvailablePoint(),
                point.getDepositPoint(),
                point.getSettlementPoint()
        );
    }

    @Override

    // 현재 보유 포인트로 입찰이 가능한지 확인하는 메서드
    public void validateSufficientPoints(Long userId, Long requiredAmount) {

        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new IllegalArgumentException("User does not have a point account yet.")
        );

        if (point.getAvailablePoint() < requiredAmount) {
            throw new IllegalArgumentException("Insufficient available points.");
        }
    }

    // 입찰 포인트를 예치 포인트로 이동 후 포인트 로그에 기록
    @Override
    public void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId) {

        // 포인트 조회
        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new IllegalArgumentException("User does not have a point account yet.")
        );

        // 가용 포인트가 충분한지 검증
        if (point.getAvailablePoint() < bidAmount) {
            throw new IllegalArgumentException("Insufficient available points.");
        }

        // 포인트 이동 (가용 -> 예치)
        point.updateDepositPoint(bidAmount);

        // 포인트 로그에 기록
        PointLog log = PointLog.create(
                point,
                point.getUser(),
                PointLogType.DEPOSIT,
                "입찰금 예치 처리",
                bidAmount
        );

        pointLogRepository.save(log);
    }

}
