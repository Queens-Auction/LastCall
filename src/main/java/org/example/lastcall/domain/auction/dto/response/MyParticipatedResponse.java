package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Schema(description = "내가 참여한 경매 조회 응답 DTO")
@Getter
public class MyParticipatedResponse {
    @Schema(description = "경매 ID", example = "101")
    private final Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private final String imageUrl;

    @Schema(description = "상품 이름", example = "맥북 프로 16인치 M3")
    private final String productName;

    @Schema(description = "상품 상세 설명", example = "M3 칩셋이 탑재된 맥북 프로 16인치, 미개봉 제품입니다.")
    private final String productDescription;

    @Schema(description = "현재 최고 입찰가", example = "1500000")
    private final Long currentBid;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
    private final AuctionStatus status;

    @Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
    private final LocalDateTime startTime;

    @Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
    private final LocalDateTime endTime;

    @Schema(description = "현재 최고 입찰자가 나인지 여부", example = "true")
    private final Boolean isLeading;

    @Schema(description = "내가 해당 경매에서 넣은 최고 입찰가 (단건 조회 시에만 표시)", example = "1550000")
    private final Long myBidAmount;

    public MyParticipatedResponse(Long id, String imageUrl, String productName, String productDescription, Long currentBid,
                                  AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime, Boolean isLeading, Long myBidAmount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.productName = productName;
        this.productDescription = productDescription;
        this.currentBid = currentBid;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isLeading = isLeading;
        this.myBidAmount = myBidAmount;
    }

    public static MyParticipatedResponse from(Auction auction, Product product, String imageUrl, Long currentBid, Boolean isLeading) {
        return new MyParticipatedResponse(
                auction.getId(),
                imageUrl,
                product.getName(),
                product.getDescription(),
                currentBid,
                auction.getDynamicStatus(),
                auction.getStartTime(),
                auction.getEndTime(),
                isLeading,
                null);
    }
}
