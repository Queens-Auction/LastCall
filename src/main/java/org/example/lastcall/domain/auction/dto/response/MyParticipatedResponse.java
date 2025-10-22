package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Getter
@Builder
public class MyParticipatedResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private String productDescription;
    private Long currentBid;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long myBidAmount;  // 내가 해당 경매에서 넣은 최고 입찰가 (단건 조회시에만 표시)
    private Boolean isLeading; // 경매종료 -> 나의 낙찰 여부 / 경매진행중 -> 나의 현재 최고 입찰자 여부

    // 내가 참여한 경매 목록 조회 (전체)
    public static MyParticipatedResponse from(Auction auction, Product product, String imageUrl, Long currentBid, Boolean isLeading) {
        return MyParticipatedResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .currentBid(currentBid)
                .status(auction.getDynamicStatus()) // 조회 시점 상태 계산
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .isLeading(isLeading)
                .build();
    }

    // 내가 참여한 경매 상세 조회 (단건)
    public static MyParticipatedResponse fromDetail(Auction auction, Product product, String imageUrl, Long currentBid, Long myBidAmount, Boolean isLeading) {
        return MyParticipatedResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .currentBid(currentBid)
                .status(auction.getDynamicStatus()) // 조회 시점 상태 계산
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .myBidAmount(myBidAmount)
                .isLeading(isLeading)
                .build();
    }
}
