package org.example.lastcall.domain.auction.service.command;

import lombok.RequiredArgsConstructor;
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
import org.example.lastcall.domain.point.service.query.PointQueryServiceApi;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionCommandService implements AuctionCommandServiceApi {

    private final AuctionRepository auctionRepository;
    private final UserServiceApi userServiceApi;
    private final ProductQueryServiceApi productQueryService;
    private final PointQueryServiceApi pointQueryServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final PointCommandService pointCommandServiceApi;

    // 경매 등록 //
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        productQueryService.validateProductOwner(productId, userId);
        // 2. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(productId)) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
        // 3. User 조회
        User user = userServiceApi.findById(userId);
        // 4. 상품 조회
        Product product = productQueryService.findById(productId);
        // 5. 경매 등록
        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);

        return AuctionResponse.fromCreate(auction);
    }

    // 내 경매 수정 //
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

    // 경매 상태 변경 (closed)
    public void closeAuction(Long auctionId) {
        // 1. 경매 엔티티 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. 종료 여부 검증
        if (!auction.canClose()) {
            throw new BusinessException(AuctionErrorCode.AUCTION_ALREADY_CLOSED);
        }

        // 4. 최고 입찰자 조회
        Bid topBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElse(null);

        if (topBid != null) {
            Long winnerId = topBid.getUser().getId();
            Long bidAmount = topBid.getBidAmount();

            // 낙찰자 호출
            auction.assignWinner(winnerId, bidAmount);

            // 포인트 처리 (예치 -> 가용)
            pointCommandServiceApi.depositToAvailablePoint(winnerId, auction.getId(), bidAmount);
        }
        auctionRepository.save(auction);
    }
}
