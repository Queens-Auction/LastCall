package org.example.lastcall.domain.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.query.ProductQueryServiceApi;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MyAuctionService {

    private final AuctionRepository auctionRepository;
    private final ProductQueryServiceApi productImageService;
    private final BidQueryServiceApi bidService;

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

    // 내가 참여한 경매 전체 조회 //
    public PageResponse<MyParticipatedResponse> getMyParticipatedAuctions(Long userId, Pageable pageable) {
        // 사용자가 입찰한 경매 ID 목록 조회
        List<Long> auctionIds = bidService.getParticipatedAuctionIds(userId);

        // 경매 ID 목록으로 해당 경매 페이지로 나눠 조회
        Page<Auction> auctions = auctionRepository.findByIdIn(auctionIds, pageable);

        // 각 경매에 대한 상품, 이미지, 최고 입찰가, 내 최고 입찰 여부 등 정보매핑
        Page<MyParticipatedResponse> responses = auctions.map(auction -> {
            Product product = auction.getProduct();

            String imageUrl = productImageService
                    .readThumbnailImage(product.getId())
                    .getImageUrl();

            // 최고 입찰가 조회
            Long currentBid = bidService.getCurrentBidAmount(auction.getId());

            // 내가 최고입찰자인지 여부 확인
            Boolean isLeading = bidService.isUserLeading(auction.getId(), userId);

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

        Product product = auction.getProduct();

        String imageUrl = productImageService
                .readThumbnailImage(product.getId())
                .getImageUrl();

        Long currentBid = bidService.getCurrentBidAmount(auction.getId());

        Boolean isLeading = bidService.isUserLeading(auction.getId(), userId);

        Long myBidAmount = bidService.getMyBidAmount(auction.getId(), userId);

        return MyParticipatedResponse.fromDetail(
                auction,
                product,
                imageUrl,
                currentBid,
                myBidAmount,
                isLeading
        );
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
        return AuctionResponse.from(auction);
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
        auction.softDelete();
    }
}
