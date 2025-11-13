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
import org.example.lastcall.domain.user.service.UserServiceApi;
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
    private final UserServiceApi userServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final AuctionFinder auctionFinder;
    private final AuctionRepository auctionRepository;
    private final PointLockService pointLockService;
  
    /**
     * TODO
     * 포인트 등록 (충전)
     * 어떤 결제에 의해 충전되는지 확인해서
     * 만약 결제를 진행하는 order_id 가 이미 포인트 충전에 처리되어 있으면 exception을 발생시키는 비즈니스 로직을 통해
     * 하나의 결제로 2번 이상 포인트가 충전되는 현상을 막아햐하지 그 자체를 lock 에서 제어하면 안됨.
     *
     * POINT 등록시 결제 시스템에서 OrderId (주문아이디) 로 이미 OrderId로 충전된 내역이 있는지 확인해야 함.
     * @CacheEvict: 캐시 정보를 메모리상에 삭제하는 기능 수행
     */
    @CacheEvict(value = "userPoints", key = "#authUser.userId()")
    @DistributedLock(key = "'user:' + #authUser.userId()")
    public PointResponse createPoint(AuthUser authUser, @Valid PointCreateRequest request) {
        log.debug("락 획득 후 작업 실행: 포인트 충전 요청 - userId: {}, incomePoint: {}", authUser.userId(), request.getIncomePoint());
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
        log.debug("락을 점유한 작업 종료: 포인트 충전 완료 - userId: {}, currentPoint: {}", authUser.userId(),
            currentPoint.getAvailablePoint());
        PointLog log = pointLogRepository.save(
            PointLog.create(
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
            currentPoint.getSettlementPoint()
        );
    }
  
    /**
     * 입찰 발생 시 포인트 예치 관련 변경 메서드
     * bidId를 확인해서 이미 같은 bidId로 입찰이 발생한 경우가 있으면 요 메서드의 비즈니스 로직에서 같은 bidId로 2번의 예치포인트로 변경이 발생하지 않게 만들어야지
     * lock에서 관리하는 것은 아님
     *
     * - 이미 같은 bidId로 처리된 내역이 있는지를 확인해야 한다.
     * -
     * @param auctionId
     * @param bidId
     * @param bidAmount
     * @param userId
     */
    @Override
    @CacheEvict(value = "userPoints", key = "#userId")
    @DistributedLock(key = "'user:' + #userId") // <- TODO 요녀석의 역할은 동시에 진행되는 것을 막는것이지 각 비즈니스로직에 대한 검증을 하는 것은 아니다.
    public void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId) {
        log.debug("락 획득 후 작업 실행: 입찰 포인트 예치 - userId: {}, auctionId: {}, bidAmount: {}", userId, auctionId, bidAmount);
        // 포인트 조회
        Point point = pointRepository.findByUserId(userId).orElseThrow(
            () -> new BusinessException(PointErrorCode.POINT_RECORD_NOT_FOUND)
        );
        // bid 아이디로 이미 예치 처리가 되었는지 확인함
        boolean alreadyProcessed = pointLogRepository.existsByBidIdAndTypeIn(bidId,
            List.of(PointLogType.DEPOSIT, PointLogType.ADDITIONAL_DEPOSIT));
        if (alreadyProcessed) {
            throw new BusinessException(PointErrorCode.ALREADY_PROCESSED_DEPOSIT);
        }
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
                    userId,
                    PointLogType.ADDITIONAL_DEPOSIT,
                    "입찰 금액 증가로 인한 추가 예치 처리",
                    difference,
                    auctionId,
                    bidId
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
                userId,
                PointLogType.DEPOSIT,
                "입찰금 예치 처리",
                bidAmount,
                auctionId,
                bidId
            );
            pointLogRepository.save(log);
        }
        log.debug("락을 점유한 작업 종료: 포인트 예치 완료 - userId: {}, availablePoint: {}, depositPoint: {}", userId,
            point.getAvailablePoint(), point.getDepositPoint());
    }
  
    // 경매 종료 후 입찰 확정시 예치 포인트를 정산 포인트로 이동
    /**
     * TODO auctionId로 이미 정산 처리된 내역이 있는지 확인하는 과정이 필요
     * @param userId
     * @param auctionId
     * @param amount
     */
    @Override
    public void depositToSettlement(Long userId, Long auctionId,
        Long amount) { // TODO <-- 여기서 userId와 amount 는 사용하지 않는 parameter 임, userId를 빼는 순간 @CacheEvict 를 사용할 수 없기 때문에 메서드 내에서 CacheManager 를 이용해서 처리해야 함.
        // 경매 조회
        Auction auction = auctionFinder.findById(auctionId);
        // 최고 입찰 조회
        Bid highestBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElseThrow(
            () -> new BusinessException(BidErrorCode.BID_NOT_FOUND)
        );
        // 메서드 분리함
        pointLockService.depositToSettlementToUser(auction, highestBid.getUser().getId(), highestBid.getBidAmount());
    }
  
    // 경매 종료 후 입찰에 실패한 사용자들의 예치 포인트를 가용 포인트로 이동
    @Override
    // @CacheEvict(value = "userPoints", key = "#userId")
    public void depositToAvailablePoint(Long userId, Long auctionId,
        Long amount) { // TODO <-- userId 필요 없음, amount 도 필요 없음 사용안함
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
        // select max(bid_amount), user_id from bid where auction_id = {auctionId} group by user_id;
        List<Bid> allBids = bidQueryServiceApi.findAllByAuctionId(auction.getId());
        // Set<Bid> filteredBids = allBids.stream().filter(e -> !e.getUser().getId().equals(winnerUserId))
        //  .collect(Collectors.toSet());
        //
        // filteredBids.forEach(e -> {
        //  // 메서드 분리함
        //  pointLockService.depositToAvailablePointToUser(auction, e.getUser().getId(), winnerUserId, e);
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
            pointLockService.depositToAvailablePointToUser(auction, loserId, winnerUserId, finalBid);
        }
    }
}