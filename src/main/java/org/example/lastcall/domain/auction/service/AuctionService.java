package org.example.lastcall.domain.auction.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.service.BidServiceApi;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.ProductImageViewServiceApi;
import org.example.lastcall.domain.product.sevice.ProductViewServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService implements AuctionServiceApi {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ProductViewServiceApi productService;
    // product 엔티티를 DB 조회 없이 식별자 기반 참조하기 위해 사용 (경매 등록시)
    private final EntityManager em;
    private final ProductImageServiceApi productImageService;
    //private final BidServiceApi bidService;
    private final ProductImageViewServiceApi productImageService;
    private final BidServiceApi bidService;

    // 경매 상태 분리
    private AuctionStatus determineStatus(AuctionCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(request.getStartTime())) {
            return AuctionStatus.SCHEDULED;
        } else if (now.isAfter(request.getEndTime())) {
            return AuctionStatus.CLOSED;
        } else {
            return AuctionStatus.ONGOING;
        }
    }

    // 경매 등록 //
    public AuctionCreateResponse createAuction(Long userId, AuctionCreateRequest request) {

        // 1. 상품 존재 여부 확인
        ProductResponse productResponse = productService.readProduct(request.getProductId());
        if (productResponse == null) {
            throw new BusinessException(AuctionErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 상품 소유자 검증
        if (!productResponse.getUserId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }

        // 3. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(request.getProductId())) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }

        // 4. 경매 상태 결정
        AuctionStatus status = determineStatus(request);

        // 5. User 엔티티 조회
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER)
        );

        // 6. Product 엔티티 조회 없이 참조만 (SELECT X)
        // -> Product 엔티티 내 빌더가 id 제외 되어있어서 (클래스 단위 아닌 메서드 단위)
        Product product = em.getReference(Product.class, productResponse.getId());

        // 7. 경매 등록
        Auction auction = Auction.of(
                user,
                product,
                request,
                status
        );

        auctionRepository.save(auction);

        return AuctionCreateResponse.from(auction);
    }

    // 경매 전체 조회 //
    @Transactional(readOnly = true)
    public PageResponse<AuctionReadAllResponse> readAllAuctions(Category category, Pageable pageable) {

        // 1. 경매 목록 조회 (최신순)
        Page<Auction> auctions = auctionRepository.findAllActiveAuctionsByCategory(category, pageable);

        // 2. 엔티티 -> DTO 변환
        List<AuctionReadAllResponse> responses = auctions.stream()
                .map(auction -> {
                    // 현재 경매에 연결된 상품의 이미지 조회
                    ProductImageResponse image = productImageService.readThumbnailImage(auction.getProduct().getId());

                    return AuctionReadAllResponse.from(auction, image.getImageUrl());
                })
                .toList();

        // 3. PageResponse로 변환하여 페이징 응답 반환
        return PageResponse.of(auctions, responses);
    }

    // 경매 단건 상세 조회 //
    @Transactional(readOnly = true)
    public AuctionReadResponse readAuction(Long auctionId, Long userId) {

        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. 상품 이미지 조회
        List<ProductImageResponse> images = productImageService.readAllProductImage(auction.getProduct().getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        // 3. 경매에 내 참여 여부 조회 - 입찰 도메인 호출
        boolean myParicipsted = false;
        // 비즈 서비스 api에 existsByAuctionIdAndUserId 추가되면 주석풀기
        //if (userId != null) {
        //    myParicipsted = bidService.existsByAuctionIdAndUserId(auctionId, userId);
        //}

        return AuctionReadResponse.from(
                auction,
                auction.getProduct(),
                imageUrl,
                myParicipsted
        );
    }

    // Override //

    // 특정 상품에 진행 중인 경매 여부 검증
    @Override
    public void validateAuctionScheduled(Long productId) {
        boolean isScheduledAuction = auctionRepository.existsByProductIdAndStatus(productId, AuctionStatus.SCHEDULED);

        if (!isScheduledAuction) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION);
        }
    }

    // 입찰 가능한 경매 여부 검증
    @Override
    public Auction getBiddableAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() == AuctionStatus.SCHEDULED
                || auction.getStatus() == AuctionStatus.CLOSED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_BID_ON_NON_ONGOING_AUCTION);
        }
        return auction;
    }

    // 경매 ID로 경매 조회
    @Override
    public Auction findById(Long auctionId) {
        return auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND)
        );
    }
}
