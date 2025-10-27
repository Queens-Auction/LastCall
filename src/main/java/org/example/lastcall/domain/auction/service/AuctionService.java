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
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService implements AuctionServiceApi {
    private final AuctionRepository auctionRepository;
    private final UserServiceApi userService;
    private final ProductQueryServiceApi productService;
    //private final BidQueryService bidService;

    // 경매 등록 //
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        // productService.validateProductOwner(productId, userId); 연결되면 주석 풀기
        // 2. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(productId)) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
        // 3. User 조회
        User user = userService.findById(userId);
        // 4. 상품 조회
        Product product = productService.findById(productId);
        // 5. 경매 등록
        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);

        return AuctionResponse.fromCreate(auction);
    }

    // 경매 전체 조회 //
    @Transactional(readOnly = true)
    public PageResponse<AuctionReadAllResponse> getAllAuctions(Category category, Pageable pageable) {
        // 1. 경매 목록 조회 (최신순)
        Page<Auction> auctions = auctionRepository.findAllActiveAuctionsByCategory(category, pageable);
        // 2. 엔티티 -> DTO 변환
        List<AuctionReadAllResponse> responses = auctions.stream()
                .map(auction -> {
                    // 현재 경매에 연결된 상품의 이미지 조회
                    ProductImageResponse image = productService.readThumbnailImage(auction.getProduct().getId());
                    // 참여자(입찰자) 수 계산
                    //int participantCount = bidService.getParticipantCount(auction.getId()); 연결되면 주석풀기 (cqrs 구조 변경 후 진행해야함)
                    int participantCount = 0;

                    return AuctionReadAllResponse.from(auction, image.getImageUrl(), participantCount);
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
    public AuctionReadResponse getAuction(Long auctionId, Long userId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        // 2. 상품 이미지 조회
        List<ProductImageResponse> images = productService.readAllProductImage(auction.getProduct().getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        boolean participated = false;
        // 공개용/로그인용 같이 쓰기 -> cqrs 분리 후 주석풀기
        //if (userId != null) {
        //    participated = bidService.existsByAuctionIdAndUserId(auctionId, userId);
        //}

        return AuctionReadResponse.from(
                auction,
                auction.getProduct(),
                imageUrl,
                participated
        );
    }

    // 상품 수정 시, 해당 상품이 연결된 경매 확인 후 수정 가능 여부 검증
    @Override
    public void validateAuctionStatusForModification(Long productId) {
        // 해당 상품과 연결된 경매가 있는지 조회
        Optional<Auction> auctionOpt = auctionRepository.findByProductId(productId);
        // 경매가 없으면 통과
        if (auctionOpt.isEmpty()) {
            return;
        }
        AuctionStatus status = auctionOpt.get().getStatus();
        // 경매가 진행중(ONGOING) 또는 종료(CLOSED)면 수정 불가
        if (status == AuctionStatus.ONGOING || status == AuctionStatus.CLOSED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION);
        }
        // SCHEDULED면 통과
    }

    // 입찰 가능한 경매 여부 검증
    @Override
    public Auction getBiddableAuction(Long auctionId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
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
