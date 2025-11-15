package org.example.lastcall.domain.auction.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.lock.DistributedLock;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.point.service.command.PointCommandService;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuctionCommandService {
    private final AuctionRepository auctionRepository;
    private final UserQueryServiceApi userQueryServiceApi;
    private final ProductQueryServiceApi productQueryServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final PointCommandService pointCommandServiceApi;
    private final AuctionEventScheduler auctionEventScheduler;

    // 경매 등록
    @DistributedLock(key = "'product:' + #productId")
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        log.debug("[RedissonLock] 락 획득 후 작업 실행: 경매 등록 처리 시작 - productId={}", productId);

        Product product = productQueryServiceApi.validateProductOwner(productId, userId);
        User user = product.getUser();
        log.debug("[RedissonLock] 상품 소유자 검증 완료 - productId={}, userId={}", productId, userId);

        if (auctionRepository.existsActiveAuction(productId)) {
            log.warn("[RedissonLock] 이미 활성화된 경매 존재 - productId={}", productId);
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
//의미X -> 시작일 이후 조건만 있으면 지금보다 이후인건 자동 보장됨
//        if (!request.getEndTime().isAfter(LocalDateTime.now())) {
//            throw new BusinessException(AuctionErrorCode.INVALID_END_TIME);
//        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_END_TIME_ORDER);
        }

        if (request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_SAME_TIME);
        }

        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);
        log.info("[RedissonLock] 경매 생성 완료 - auctionId={}, productId={}, startPrice={}, endTime={}", auction.getId(), productId, auction.getStartingBid(), auction.getEndTime());

        auctionEventScheduler.scheduleAuctionEvents(auction);
        log.info("경매 등록 완료 및 이벤트 예약 - auctionId={}, startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());
        log.info("[RedissonLock] 락 점유한 작업 종료 - productId={}", productId);

        return AuctionResponse.fromCreate(auction);
    }

    // 내 경매 수정
    public AuctionResponse updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }

        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION);
        }

        auction.update(request);
        auction.increaseVersion();

        auctionRepository.save(auction);
        log.info("경매 수정됨 - auctionId={},startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());

        auctionEventScheduler.rescheduleAuctionEvents(auction);

        return AuctionResponse.fromUpdate(auction);
    }

    // 내 경매 삭제
    public void deleteAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }

        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION);
        }

        auction.markAsDeleted();
    }

    // 경매 종료 처리 (closed)
    @DistributedLock(key = "'auction:' + #auctionId")
    public void closeAuction(Long auctionId) {
        log.debug("[RedissonLock] 락 획득 후 작업 실행: 경매 종료 처리 시작 - auctionId={}", auctionId);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.canClose()) {
            log.warn("[RedissonLock] 이미 종료된 경매 - auctionId={}", auctionId);
            throw new BusinessException(AuctionErrorCode.AUCTION_ALREADY_CLOSED);
        }

        Bid topBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElse(null);

        if (topBid != null) {
            Long winnerId = topBid.getUser().getId();
            Long bidAmount = topBid.getBidAmount();
            log.debug("[RedissonLock] 낙찰 처리 진행 - auctionId={}, winnerId={}, bidAmount={}", auctionId, winnerId, bidAmount);

            auction.assignWinner(winnerId, bidAmount);

            pointCommandServiceApi.depositToAvailablePoint(auction.getId());
            pointCommandServiceApi.depositToSettlement(auction.getId());

            log.info("[RedissonLock] 경매 종료 - 낙찰자 id: {}, 낙찰가: {}원", winnerId, bidAmount);
        } else {
            auction.closeAsFailed();
            log.info("[RedissonLock] 경매 종료 - 입찰 없음(유찰 처리): auctionId={}", auctionId);
        }

        auctionRepository.save(auction);
        log.info("[RedissonLock] 락 점유한 작업 종료 - auctionId={}", auctionId);
    }

    // 경매 시작 후 상태 변경 (SCHEDULED -> ONGOING)
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            log.warn("경매 시작 불가: auctionId={}", auctionId);

            return;
        }

        auction.updateStatus(AuctionStatus.ONGOING);
        auctionRepository.save(auction);
        log.info("경매 시작 상태로 변경 완료: auctionId={}, startTime={}", auctionId, auction.getStartTime());
    }
}
