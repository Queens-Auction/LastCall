package org.example.lastcall.domain.auction.service.query;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionQueryService implements AuctionQueryServiceApi {
    private final AuctionRepository auctionRepository;
    private final ProductQueryServiceApi productQueryServiceApi;
    private final BidQueryServiceApi bidQueryServiceApi;

    // 경매 전체 조회
    public PageResponse<AuctionReadAllResponse> getAllAuctions(Category category, Pageable pageable) {
        Page<AuctionReadAllResponse> auctions = auctionRepository.findAllAuctionSummaries(category, pageable);

        return PageResponse.of(auctions);
    }

    // 경매 단건 상세 조회 //
    // 로그인 하지 않은 사용자도 접근 가능
    public AuctionReadResponse getAuction(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        List<ProductImageResponse> images = productQueryServiceApi.findAllProductImage(auction.getProduct().getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        boolean participated = false;
        boolean canBid = false;
        if (userId != null) {
            participated = bidQueryServiceApi.existsByAuctionIdAndUserId(auctionId, userId);

            boolean isNotOwner = !auction.getProduct().getUser().getId().equals(userId);
            boolean isOngoing = auction.getDynamicStatus() == AuctionStatus.ONGOING;
            canBid = isNotOwner && isOngoing;
        }

        Long currentBid = bidQueryServiceApi.findCurrentBidAmount(auction.getId());

        return AuctionReadResponse.from(
                auction,
                auction.getProduct(),
                imageUrl,
                currentBid,
                participated,
                canBid
        );
    }

    // 내가 판매한 경매 목록 조회
    public PageResponse<MySellingResponse> getMySellingAuctions(Long userId, Pageable pageable) {
        Page<MySellingResponse> auctions = auctionRepository.findMySellingAuctions(userId, pageable);

        return PageResponse.of(auctions);
    }

    // 내가 판매한 경매 상세 조회
    public MySellingResponse getMySellingDetailAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findBySellerIdAndAuctionId(userId, auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        Product product = auction.getProduct();

        String imageUrl = productQueryServiceApi
                .findThumbnailImage(product.getId())
                .getImageUrl();

        Long currentBid = bidQueryServiceApi.findCurrentBidAmount(auction.getId());

        return MySellingResponse.from(
                auction,
                product,
                imageUrl,
                currentBid);
    }

    // 내가 참여한 경매 전체 조회
    public PageResponse<MyParticipatedResponse> getMyParticipatedAuctions(Long userId, Pageable pageable) {
        Page<MyParticipatedResponse> page = auctionRepository.findMyParticipatedAuctions(userId, pageable);

        return PageResponse.of(page);
    }

    // 내가 참여한 경매 단건 조회
    public MyParticipatedResponse getMyParticipatedDetailAuction(Long userId, Long auctionId) {
        return auctionRepository.findMyParticipatedAuctionDetail(auctionId, userId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    // 상품 수정 시, 해당 상품이 연결된 경매 확인 후 수정 가능 여부 검증
    @Override
    public void validateAuctionStatusForModification(Long productId) {
        Optional<Auction> auctionOpt = auctionRepository.findByProductId(productId);

        if (auctionOpt.isEmpty()) {
            return;
        }

        AuctionStatus status = auctionOpt.get().getStatus();

        if (!status.equals(AuctionStatus.SCHEDULED)) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION);
        }
    }

    // 입찰 가능한 경매 여부 검증
    @Override
    public Auction findBiddableAuction(Long auctionId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getStatus().equals(AuctionStatus.ONGOING)) {
            throw new BusinessException(AuctionErrorCode.CANNOT_BID_ON_NON_ONGOING_AUCTION);
        }

        return auction;
    }
}
