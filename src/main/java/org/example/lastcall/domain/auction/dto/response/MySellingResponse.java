package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Schema(description = "내가 판매한 경매 조회 응답 DTO")
@Getter
@Builder
public class MySellingResponse {
    @Schema(description = "경매 ID", example = "101")
    private Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private String imageUrl;

    @Schema(description = "상품 이름", example = "아이폰 15 프로 256GB")
    private String productName;

    @Schema(description = "상품 상세 설명", example = "미개봉 새 제품, 그레이 색상 모델입니다.")
    private String productDescription;

    @Schema(description = "현재 최고 입찰가", example = "1250000")
    private Long currentBid;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
    private AuctionStatus status;

    @Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
    private LocalDateTime endTime;

    // 정적 팩토리 메서드 (from)
    public static MySellingResponse from(Auction auction, Product product, String imageUrl, Long currentBid) {
        return MySellingResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .currentBid(currentBid)
                .status(auction.getDynamicStatus()) // 조회 시점 상태 계산
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .build();
    }
}
