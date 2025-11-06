package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Schema(description = "내가 참여한 경매 조회 응답 DTO")
@Getter
@Builder
// 둘 다 필요! (없으면 에러 발생)
@AllArgsConstructor
@NoArgsConstructor
public class MyParticipatedResponse {
    @Schema(description = "경매 ID", example = "101")
    private Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private String imageUrl;

    @Schema(description = "상품 이름", example = "맥북 프로 16인치 M3")
    private String productName;

    @Schema(description = "상품 상세 설명", example = "M3 칩셋이 탑재된 맥북 프로 16인치, 미개봉 제품입니다.")
    private String productDescription;

    @Schema(description = "현재 최고 입찰가", example = "1500000")
    private Long currentBid;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
    private AuctionStatus status;

    @Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
    private LocalDateTime endTime;

    @Schema(description = "내가 해당 경매에서 넣은 최고 입찰가 (단건 조회 시에만 표시)", example = "1550000")
    private Long myBidAmount;  // 내가 해당 경매에서 넣은 최고 입찰가 (단건 조회시에만 표시)

    @Schema(description = "현재 최고 입찰자가 나인지 여부", example = "true")
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
