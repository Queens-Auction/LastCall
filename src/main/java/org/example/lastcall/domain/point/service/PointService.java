package org.example.lastcall.domain.point.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.point.dto.request.PointCreateRequest;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.entity.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService implements PointServiceApi {

    private final PointRepository pointRepository;
    private final PointLogRepository pointLogRepository;
    private final UserServiceApi userServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final AuctionServiceApi auctionServiceApi;

    public PointResponse createPoint(AuthUser authUser, @Valid PointCreateRequest request) {

        User user = userServiceApi.findById(authUser.userId());

        Point currentPoint = pointRepository.findByUser(user).orElse(null);

        Long incomePoint = request.getIncomePoint();

        if (currentPoint == null) {
            currentPoint = Point.create(user, incomePoint);
            currentPoint = pointRepository.save(currentPoint);
        } else {
            currentPoint.updateAvailablePoint(incomePoint);
        }

        PointLog log = pointLogRepository.save(
                PointLog.create(currentPoint, user, PointLogType.EARN, PointLogType.EARN.getDescription(), incomePoint));

        return new PointResponse(
                user.getId(),
                currentPoint.getId(),
                currentPoint.getAvailablePoint(),
                currentPoint.getDepositPoint(),
                currentPoint.getSettlementPoint()
        );
    }

    // 유저 포인트 조회
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

    // 입찰 발생 시 포인트 예치 관련 변경 메서드
    @Override
    public void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId) {
        // 포인트 조회
        Point point = pointRepository.findByUserId(userId).orElseThrow(
                () -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
        );

        // 이전 입찰 조회 (해당 유저가 이미 입찰했는지)
        Optional<Bid> existingBid = bidQueryServiceApi.findLastBidExceptBidId(auctionId, userId, bidId);

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
                        PointLogType.ADDITIONAL_DEPOSIT,
                        "입찰 금액 증가로 인한 추가 예치 처리",
                        difference,
                        auctionServiceApi.findById(auctionId)
                );

                // 포인트 로그에 저장
                pointLogRepository.save(log);
            }
        } else {
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
                    bidAmount,
                    auctionServiceApi.findById(auctionId)
            );
            pointLogRepository.save(log);
        }
    }

    // 경매 종료 후 입찰 확정시 예치 포인트를 정산 포인트로 이동
    @Override
    public void depositToSettlement(Long userId, Long auctionId, Long amount) {
        // 경매 및 최고 입찰 조회
        Auction auction = auctionServiceApi.findById(auctionId);
        Bid highestBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElseThrow(
                () -> new BusinessException(BidErrorCode.BID_NOT_FOUND)
        );

        Long winnerUserId = highestBid.getUser().getId();
        Long winnerBidAmount = highestBid.getBidAmount();

        // 낙찰자의 포인트 계좌 조회
        Point point = pointRepository.findByUserId(winnerUserId).orElseThrow(
                () -> new BusinessException(PointErrorCode.POINT_ACCOUNT_NOT_FOUND)
        );

        // 예치 포인트 -> 정산 포인트로 이동
        point.depositToSettlement(winnerBidAmount);

        // 변경사항 저장
        pointRepository.save(point);

        // 포인트 로그에 기록
        PointLog log = PointLog.create(
                point,
                point.getUser(),
                PointLogType.SETTLEMENT,
                "입찰 확정으로 인한 정산 포인트 이동",
                winnerBidAmount,
                auction
        );

        // 포인트 로그에 저장
        pointLogRepository.save(log);
    }
}