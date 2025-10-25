package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Schema(description = "경매 상세 조회 응답 DTO")
@Getter
@Builder
public class AuctionReadResponse {
    @Schema(description = "경매 ID", example = "101")
    private Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private String imageUrl;

    @Schema(description = "상품 이름", example = "에어팟 프로 2세대")
    private String productName;

    @Schema(description = "상품 상세 설명", example = "노이즈 캔슬링 기능을 갖춘 최신형 에어팟 프로 2세대입니다.")
    private String productDescription;

    @Schema(description = "경매 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-25T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "경매 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-26T09:00:00")
    private LocalDateTime endTime;

    @Schema(description = "경매 시작 가격", example = "10000")
    private Long startingBid;

    @Schema(description = "입찰 단위 (입찰 시 증가 금액)", example = "1000")
    private Long bidStep;

    @Schema(description = "로그인 사용자가 해당 경매에 참여했는지 여부", example = "true")
    private Boolean myParticipated;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
    private AuctionStatus status;

    // 정적 팩토리 메서드 (from)
    public static AuctionReadResponse from(
            Auction auction,
            Product product,
            String imageUrl,
            boolean myParticipated
    ) {
        return AuctionReadResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .startingBid(auction.getStartingBid())
                .bidStep(auction.getBidStep())
                .myParticipated(myParticipated)
                .status(auction.getDynamicStatus()) // 조회 시점 상태 계산
                .build();
    }
}
