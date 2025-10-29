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
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.query.ProductQueryServiceApi;
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

    // 경매 전체 조회 //
    public PageResponse<AuctionReadAllResponse> getAllAuctions(Category category, Pageable pageable) {
        // 1. 경매 목록 조회 (최신순)
        Page<Auction> auctions = auctionRepository.findAllActiveAuctionsByCategory(category, pageable);
        // 2. 엔티티 -> DTO 변환
        List<AuctionReadAllResponse> responses = auctions.stream()
                .map(auction -> {
                    // 현재 경매에 연결된 상품의 이미지 조회
                    ProductImageResponse image = productQueryServiceApi.readThumbnailImage(auction.getProduct().getId());
                    // 참여자(입찰자) 수 계산
                    int participantCount = bidQueryServiceApi.countDistinctParticipants(auction.getId());

                    return AuctionReadAllResponse.from(auction, image.getImageUrl(), participantCount);
                })
                .toList();
        // 3. PageResponse로 변환하여 페이징 응답 반환
        return PageResponse.of(auctions, responses);
    }

    // 경매 단건 상세 조회 //
    // 로그인 하지 않은 사용자도 접근 가능
    public AuctionReadResponse getAuction(Long auctionId, Long userId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        // 2. 상품 이미지 조회
        List<ProductImageResponse> images = productQueryServiceApi.readAllProductImage(auction.getProduct().getId());
        String imageUrl = images.isEmpty() ? null : images.get(0).getImageUrl();

        boolean participated = false;
        // 공개용/로그인용 같이 쓰기
        if (userId != null) {
            participated = bidQueryServiceApi.existsByAuctionIdAndUserId(auctionId, userId);
        }
        return AuctionReadResponse.from(
                auction,
                auction.getProduct(),
                imageUrl,
                participated
        );
    }

    // 내가 판매한 경매 목록 조회 //
    @Transactional(readOnly = true)
    public PageResponse<MySellingResponse> getMySellingAuctions(Long userId, Pageable pageable) {
        // 1. 경매 목록 조회
        Page<Auction> auctions = auctionRepository.findBySellerId(userId, pageable);
        // 2. DTO 변환
        Page<MySellingResponse> responses = auctions.map(auction -> {
            Product product = auction.getProduct();
            // 썸네일 이미지 조회
            String imageUrl = productQueryServiceApi
                    .readThumbnailImage(product.getId())
                    .getImageUrl();
            // 최고 입찰가 조회
            Long currentBid = bidQueryServiceApi.getCurrentBidAmount(auction.getId());

            return MySellingResponse.from(
                    auction,
                    product,
                    imageUrl,
                    currentBid
            );
        });
        return PageResponse.of(responses);
    }

    // 내가 판매한 경매 상세 조회 //
    public MySellingResponse getMySellingDetailAuction(Long userId, Long auctionId) {
        // 본인이 등록한 경매 중 해당 ID 찾기
        Auction auction = auctionRepository.findBySellerIdAndAuctionId(userId, auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 상품 정보 가져오기
        Product product = auction.getProduct();

        // 썸네일 가져오기
        String imageUrl = productQueryServiceApi
                .readThumbnailImage(product.getId())
                .getImageUrl();

        // 최고 입찰가
        Long currentBid = bidQueryServiceApi.getCurrentBidAmount(auction.getId());

        return MySellingResponse.from(
                auction,
                product,
                imageUrl,
                currentBid
        );
    }

    // 내가 참여한 경매 전체 조회 //
    public PageResponse<MyParticipatedResponse> getMyParticipatedAuctions(Long userId, Pageable pageable) {
        // 사용자가 입찰한 경매 ID 목록 조회
        List<Long> auctionIds = bidQueryServiceApi.getParticipatedAuctionIds(userId);

        // 경매 ID 목록으로 해당 경매 페이지로 나눠 조회
        Page<Auction> auctions = auctionRepository.findByIdIn(auctionIds, pageable);

        // 각 경매에 대한 상품, 이미지, 최고 입찰가, 내 최고 입찰 여부 등 정보매핑
        Page<MyParticipatedResponse> responses = auctions.map(auction -> {
            Product product = auction.getProduct();

            String imageUrl = productQueryServiceApi
                    .readThumbnailImage(product.getId())
                    .getImageUrl();

            // 최고 입찰가 조회
            Long currentBid = bidQueryServiceApi.getCurrentBidAmount(auction.getId());

            // 내가 최고입찰자인지 여부 확인
            Boolean isLeading = bidQueryServiceApi.isUserLeading(auction.getId(), userId);

            return MyParticipatedResponse.from(
                    auction,
                    product,
                    imageUrl,
                    currentBid,
                    isLeading
            );
        });
        return PageResponse.of(responses);
    }

    // 내가 참여한 경매 단건 조회 //
    public MyParticipatedResponse getMyParticipatedDetailAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 사용자 참여 여부 검증
        boolean participated = bidQueryServiceApi.existsByAuctionIdAndUserId(auctionId, userId);
        if (!participated) {
            throw new BusinessException(AuctionErrorCode.USER_NOT_PARTICIPATED_IN_AUCTION);
        }

        Product product = auction.getProduct();

        String imageUrl = productQueryServiceApi
                .readThumbnailImage(product.getId())
                .getImageUrl();

        Long currentBid = bidQueryServiceApi.getCurrentBidAmount(auction.getId());

        Boolean isLeading = bidQueryServiceApi.isUserLeading(auction.getId(), userId);

        Long myBidAmount = bidQueryServiceApi.getMyBidAmount(auction.getId(), userId);

        return MyParticipatedResponse.fromDetail(
                auction,
                product,
                imageUrl,
                currentBid,
                myBidAmount,
                isLeading
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
}
