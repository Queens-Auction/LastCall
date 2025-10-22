package org.example.lastcall.domain.point.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.dto.CreatePointRequest;
import org.example.lastcall.domain.point.dto.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.entity.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional
public class PointService implements PointServiceApi {

    private final PointRepository pointRepository;
    private final PointLogRepository pointLogRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public PointResponse createPoint(Long userId, @Valid CreatePointRequest request) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(PointErrorCode.USER_NOT_FOUND)
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
                () -> new BusinessException(PointErrorCode.USER_NOT_FOUND)
        );

        Point point = pointRepository.findByUserId(userId).orElseThrow(
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

    @Override

    // 현재 보유 포인트로 입찰이 가능한지 확인하는 메서드
    public void validateSufficientPoints(Long userId, Long requiredAmount) {

        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException(PointErrorCode.POINT_ACCOUNT_NOT_FOUND)
        );

        if (point.getAvailablePoint() < requiredAmount) {
            throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
        }
    }

    // 입찰 발생 시 포인트 예치 관련 변경 메서드
    @Override
    public void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId) {

        // 포인트 조회
        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
        );

        // 이전 입찰 조회 (해당 유저가 이미 입찰했는지)
        Optional<Bid> existingBid = bidRepository.findByAuctionIdAndUserId(auctionId, userId);

        if (existingBid.isPresent()) {
            Bid previousBid = existingBid.get();
            Long previousBidAmount = previousBid.getBidAmount();

            // 새 금액이 이전 금액보다 큰 경우(금액 올릴 때)
            if (bidAmount > previousBidAmount) {
                Long difference = bidAmount - previousBidAmount;

                // 근데 추가하려는 금액보다 가용 포인트가 적을 경우
                if (point.getAvailablePoint() < difference) {
                    throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
                }

                // 그렇지 않다면 가용 포인트에서 차액만큼 차감
                point.decreaseAvailablePoint(difference);

                // 예치 포인트에 차액만큼 추가
                point.increaseDepositPoint(difference);

                // 포인트 로그에 기록
                PointLog log = PointLog.create(
                        point,
                        point.getUser(),
                        PointLogType.ADDITIONAL_DIPOSIT,
                        "입찰 금액 증가로 인한 추가 예치 처리",
                        difference
                );
            }
        }


        // 처음 입찰하는 경우 (전체 금액 예치)
        if (point.getAvailablePoint() < bidAmount) {
            throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
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
