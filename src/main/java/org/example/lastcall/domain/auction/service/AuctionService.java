package org.example.lastcall.domain.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService implements AuctionServiceApi {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ProductQueryServiceApi productService;

    // 경매 등록 //
    public AuctionResponse createAuction(Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        Product product = productService.findById(request.getProductId());
        // 2. 상품 소유자 검증
        if (!product.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }
        // 3. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(request.getProductId())) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
        // 4. User 엔티티 조회
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER)
        );
        // 5. 경매 등록
        Auction auction = Auction.of(
                user,
                product,
                request
        );
        auctionRepository.save(auction);

        return AuctionResponse.from(auction);
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
                    ProductImageResponse image = productService.readThumbnailImage(auction.getProduct().getId());
                    return AuctionReadAllResponse.from(auction, image.getImageUrl());
                })
                .toList();

        // 3. PageResponse로 변환하여 페이징 응답 반환
        return PageResponse.of(auctions, responses);
    }

    // 경매 단건 상세 조회 - 공개용 //
    // 로그인 하지 않은 사용자도 접근 가능
    // 공개용 , 로그인 전용 API 분리한 이유 : 순환 참조 방지
    // 여기서 bidService 호출 시 순환참조 발생
    @Transactional(readOnly = true)
    public AuctionReadResponse readAuction(Long auctionId, Long userId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        // 2. 상품 이미지 조회
        List<ProductImageResponse> images = productService.readAllProductImage(auction.getProduct().getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        return AuctionReadResponse.from(
                auction,
                auction.getProduct(),
                imageUrl,
                false // 항상 false 로 전달 -> 공개용 임을 명시
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
    public Auction getUserId(Long auctionId) {
        return auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND)
        );
    }
}
