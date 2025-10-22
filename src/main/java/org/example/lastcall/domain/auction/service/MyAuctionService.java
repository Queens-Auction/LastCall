package org.example.lastcall.domain.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.service.BidServiceApi;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.ProductImageViewServiceApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MyAuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductImageViewServiceApi productImageService;
    private final BidServiceApi bidService; // 추가

    // 내가 판매한 경매 목록 조회 //
    @Transactional(readOnly = true)
    public PageResponse<MySellingResponse> getMySellingAuctions(Long userId, Pageable pageable) {
        // 1. 경매 목록 조회
        Page<Auction> auctions = auctionRepository.findBySellerId(userId, pageable);

        // 2. DTO 변환
        Page<MySellingResponse> responses = auctions.map(auction -> {
            Product product = auction.getProduct();

            // 썸네일 이미지 조회
            String imageUrl = productImageService
                    .readThumbnailImage(product.getId())
                    .getImageUrl();

            // 최고 입찰가 조회
            Long currentBid = bidService.getCurrentBidAmount(auction.getId());

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
        String imageUrl = productImageService
                .readThumbnailImage(product.getId())
                .getImageUrl();

        // 최고 입찰가
        Long currentBid = bidService.getCurrentBidAmount(auction.getId());

        return MySellingResponse.from(
                auction,
                product,
                imageUrl,
                currentBid
        );
    }
}
