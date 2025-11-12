package org.example.lastcall.domain.auction.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
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
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j // 추가
public class AuctionCommandService implements AuctionCommandServiceApi {

    private final AuctionRepository auctionRepository;
    private final UserServiceApi userServiceApi;
    private final ProductQueryServiceApi productQueryServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final PointCommandService pointCommandServiceApi;
    private final AuctionEventScheduler auctionEventScheduler;

    // 경매 등록 //
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        productQueryServiceApi.validateProductOwner(productId, userId);
        // 2. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(productId)) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
        // 3. User 조회
        User user = userServiceApi.findById(userId);
        // 4. 상품 조회
        Product product = productQueryServiceApi.findById(productId);
        // 5. Auction 엔티티 생성/저장 -> 경매 상태는 내부 로직에서 자동 계산됨
        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);

        // 이벤트 스케줄링 분리
        // 경매 시작/종료 이벤트 예약 발행
        auctionEventScheduler.scheduleAuctionEvents(auction);

        log.info("경매 등록 완료 및 이벤트 예약 - auctionId={}, startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());

        return AuctionResponse.fromCreate(auction);
    }

    // 내 경매 수정 //
    // 수정 시 이벤트 재발행
    public AuctionResponse updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        // 수정 권한 검증
        if (!auction.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }
        // 진행중/종료된 경매는 수정 불가
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION);
        }
        // 수정 관련 이벤트 반영 //
        auction.update(request);
        // 버전 증가 (수정시마다 +1 증가되어, 이전 이벤트 무시되게 해줌)
        auction.increaseVersion();
        auctionRepository.save(auction);

        log.info("경매 수정됨 - auctionId={},startTime={}, endTime={}", auction.getId(), auction.getStartTime(), auction.getEndTime());

        // 새 시간 기준으로 이벤트 재등록
        auctionEventScheduler.rescheduleAuctionEvents(auction);

        return AuctionResponse.fromUpdate(auction);
    }

    // 내 경매 삭제 //
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
    public void closeAuction(Long auctionId) {
        // 1. 경매 엔티티 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. 종료 여부 검증
        if (!auction.canClose()) {
            throw new BusinessException(AuctionErrorCode.AUCTION_ALREADY_CLOSED);
        }

        // 3. 최고 입찰자 조회
        Bid topBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElse(null);

        if (topBid != null) {
            // 입찰 존재 시, 낙찰 처리
            Long winnerId = topBid.getUser().getId();
            Long bidAmount = topBid.getBidAmount();

            // 낙찰 처리
            auction.assignWinner(winnerId, bidAmount);

            // 포인트 처리
            // 1. 낙찰자 : 예치 -> 정산
            pointCommandServiceApi.depositToSettlement(winnerId, auction.getId(), bidAmount);

            // 2. 유찰자들 : 예치 -> 가용
            pointCommandServiceApi.depositToAvailablePoint(winnerId, auction.getId(), bidAmount);

            log.info("경매 종료 - 낙찰자 id: {}, 낙찰가: {}원", winnerId, bidAmount);
        } else {
            auction.closeAsFailed();
            log.info("경매 종료 - 입찰 없음(유찰 처리): auctionId={}", auctionId);
        }
        auctionRepository.save(auction);
    }

    // 경매 시작 후 상태 변경 (SCHEDULED -> ONGOING)
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 상태 검증
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            log.warn("경매 시작 불가: auctionId={}", auctionId);
            return;
        }

        // 상태 변경
        auction.updateStatus(AuctionStatus.ONGOING);
        auctionRepository.save(auction);

        log.info("경매 시작 상태로 변경 완료: auctionId={}, startTime={}", auctionId, auction.getStartTime());
    }
}
