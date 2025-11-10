package org.example.lastcall.domain.point.service.command;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
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
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointCommandService implements PointCommandServiceApi {
    private final PointRepository pointRepository;
    private final PointLogRepository pointLogRepository;
    private final UserServiceApi userServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final AuctionFinder auctionFinder;
    private final AuctionRepository auctionRepository;

    // 포인트 등록 (충전)
    // @CacheEvict: 캐시 정보를 메모리상에 삭제하는 기능 수행
    @CacheEvict(value = "userPoints", key = "#authUser.userId()")
    public PointResponse createPoint(AuthUser authUser, @Valid PointCreateRequest request) {
        User user = userServiceApi.findById(authUser.userId());

        Point currentPoint = pointRepository.findByUser(user).orElse(null);

        Long incomePoint = request.getIncomePoint();

        PointLogType type = request.getType();

        if (currentPoint == null) {
            currentPoint = Point.create(user, type, incomePoint);
            currentPoint = pointRepository.save(currentPoint);
        } else {
            currentPoint.updateAvailablePoint(incomePoint);
        }

        PointLog log = pointLogRepository.save(
                PointLog.create(
                        currentPoint,
                        user,
                        PointLogType.EARN,
                        PointLogType.EARN.getDescription(),
                        incomePoint));

        return new PointResponse(
                user.getId(),
                currentPoint.getId(),
                currentPoint.getAvailablePoint(),
                currentPoint.getDepositPoint(),
                currentPoint.getSettlementPoint()
        );
    }

    // 입찰 발생 시 포인트 예치 관련 변경 메서드
    @Override
    @CacheEvict(value = "userPoints", key = "#userId")
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
                        auctionFinder.findById(auctionId)
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
                    auctionFinder.findById(auctionId)
            );
            pointLogRepository.save(log);
        }
    }

    // 경매 종료 후 입찰 확정시 예치 포인트를 정산 포인트로 이동
    @Override
    @CacheEvict(value = "userPoints", key = "#userId")
    public void depositToSettlement(Long userId, Long auctionId, Long amount) {
        // 경매 및 최고 입찰 조회
        Auction auction = auctionFinder.findById(auctionId);
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

    // 경매 종료 후 입찰에 실패한 사용자들의 예치 포인트를 가용 포인트로 이동
    @Override
    @CacheEvict(value = "userPoints", key = "#userId")
    public void depositToAvailablePoint(Long userId, Long auctionId, Long amount) {
        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND)
        );

        // 해당 경매의 최고 입찰자 조회
        Bid highestBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElseThrow(
                () -> new BusinessException(BidErrorCode.BID_NOT_FOUND)
        );
        Long winnerUserId = highestBid.getUser().getId();

        // 경매에 참여한 전체 입찰자 목록 조회
        List<Bid> allBids = bidQueryServiceApi.findAllByAuctionId(auction.getId());

        // 중복 입찰자는 최고 입찰가만 반영 (추가)
        Map<Long, Bid> latestBidsByUser = new HashMap<>();

        for (Bid bid : allBids) {
            Long bidderId = bid.getUser().getId();
            Bid currentHighestBid = latestBidsByUser.get(bidderId);

            if (currentHighestBid == null || bid.getBidAmount() > currentHighestBid.getBidAmount()) {
                latestBidsByUser.put(bidderId, bid);
            }
        }

        // 낙찰 실패자만 처리
        for (Map.Entry<Long, Bid> entry : latestBidsByUser.entrySet()) {
            Long loserId = entry.getKey();
            Bid finalBid = entry.getValue();

            // 낙찰자면 스킵
            if (loserId.equals(winnerUserId)) {
                continue;
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
            User user = userServiceApi.getReferenceById(loserId);

            // 포인트 로그에 기록
            pointLogRepository.save(PointLog.of(
                    point,      // 추가
                    user,
                    auction,
                    bidAmount,
                    PointLogType.DEPOSIT_TO_AVAILABLE
            ));
        }

        // 기존 코드
        // 추후 협의 후 삭제 예정
        /*for (Bid bid : allBids) {
            Long loserId = bid.getUser().getId();
            if (loserId.equals(winnerUserId))
                continue;

            Long bidAmount = bid.getBidAmount();

            Point point = pointRepository.findByUserId(loserId).orElseThrow(
                    () -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
            );

            // 예치 포인트보다 입찰 금액이 더 큰지 검사
            //if (point.getDepositPoint() < bidAmount) {
            //    throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
            //}

            // !! 현재 여기서 이 코드가 현재 금액을 통채로 덮어쓰기 하는 구조가 되어버림
            // ex) 예치 10000 / 입찰 12000 / 가용 50000 경우
            // -> 입찰금액 10000-12000 = -2000 음수로 찍혀버리고
            // 다음 연산(가용 포인트 복귀)에서 가용포인트 음수로 포인트 부족 예외 발생
            // 즉, 여러번 입찰 시 누적 업데이트는 정상 작동
            // 종료시에도 여러번 빼버리는 형태.. (종료시에는 최종 입찰가 1번만 빠져야함)
            //point.updateDepositPoint(point.getDepositPoint() - bidAmount);
            //point.updateAvailablePoint(point.getAvailablePoint() + bidAmount);
            //pointRepository.save(point);

            if (!point.canMoveDepositToAvailable(bidAmount)) {
                throw new BusinessException(PointErrorCode.INSUFFICIENT_DEPOSIT_POINT);
            }

            // 예치 -> 가용 이동
            point.moveDepositToAvailable(bidAmount);
            pointRepository.save(point);

            // 유저 객체 -> 추가
            // 유찰 대상자 불러오기
            User user = userServiceApi.findById(loserId);

            // 포인트 로그에 기록
            pointLogRepository.save(PointLog.of(
                    point,      // 추가
                    user,
                    auction,
                    bidAmount,
                    PointLogType.DEPOSIT_TO_AVAILABLE
            ));
        }*/
    }
}