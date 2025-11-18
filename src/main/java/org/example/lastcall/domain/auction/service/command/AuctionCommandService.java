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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuctionCommandService {
    private final AuctionRepository auctionRepository;
    private final ProductQueryServiceApi productQueryServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final PointCommandService pointCommandServiceApi;
    private final AuctionEventScheduler auctionEventScheduler;

    // 경매 등록
    @DistributedLock(key = "'product:' + #productId")
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        log.debug("락 획득 후 작업 실행: 경매 등록 처리 시작 - productId={}", productId);

        Product product = productQueryServiceApi.validateProductOwner(productId, userId);
        User user = product.getUser();
        log.debug("상품 소유자 검증 완료: productId={}, userId={}", productId, userId);

        if (auctionRepository.existsActiveAuction(productId)) {
            log.warn("이미 활성화된 경매 존재: productId={}", productId);
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_END_TIME_ORDER);
        }

        if (request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_SAME_TIME);
        }

        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);
        log.debug("[RabbitMQ] 경매 생성 완료: auctionId={}, productId={}, startPrice={}, endTime={}", auction.getId(), productId, auction.getStartingBid(), auction.getEndTime());

        auctionEventScheduler.scheduleAuctionEvents(auction);
        log.info("[RabbitMQ] 경매 등록 완료 및 이벤트 예약: auctionId={}, startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());
        log.debug("락 점유한 작업 종료: productId={}", productId);

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

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_END_TIME_ORDER);
        }

        if (request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessException(AuctionErrorCode.INVALID_SAME_TIME);
        }

        auction.update(request);
        auction.increaseVersion();

        auctionRepository.save(auction);
        log.info("[RabbitMQ] 경매 수정 완료: auctionId={},startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());

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
        log.debug("락 획득 후 작업 실행: 경매 종료 처리 시작 - auctionId={}", auctionId);

        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.canClose()) {
            log.warn("이미 종료된 경매: auctionId={}", auctionId);
            throw new BusinessException(AuctionErrorCode.AUCTION_ALREADY_CLOSED);
        }

        Bid topBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElse(null);

        if (topBid != null) {
            Long winnerId = topBid.getUser().getId();
            Long bidAmount = topBid.getBidAmount();
            log.debug("낙찰 처리: auctionId={}, winnerId={}, bidAmount={}", auctionId, winnerId, bidAmount);

            auction.assignWinner(winnerId, bidAmount);

            try {
                pointCommandServiceApi.depositToAvailablePoint(auction.getId());
                pointCommandServiceApi.depositToSettlement(auction.getId());
            } catch (Exception e) {
                log.error("포인트 처리 실패: auctionId={}, error={}", auctionId, e.getMessage());
            }

            log.info("[RabbitMQ] 경매 종료(낙찰): auctionId={}, winnerId={}, bidAmount={}원", auctionId, winnerId, bidAmount);
        } else {
            auction.closeAsFailed();
            log.info("[RabbitMQ] 경매 종료(유찰): auctionId={}", auctionId);
        }

        auctionRepository.save(auction);
        log.debug("락 점유한 작업 종료: auctionId={}", auctionId);
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
        log.info("[RabbitMQ] 경매 시작: auctionId={}, startTime={}", auctionId, auction.getStartTime());
    }
}
