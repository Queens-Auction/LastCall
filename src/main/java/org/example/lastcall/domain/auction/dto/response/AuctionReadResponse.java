package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Schema(description = "경매 상세 조회 응답 DTO")
@Getter
public class AuctionReadResponse {
    @Schema(description = "경매 ID", example = "101")
    private final Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private final String imageUrl;

    @Schema(description = "상품 이름", example = "에어팟 프로 2세대")
    private final String productName;

    @Schema(description = "상품 상세 설명", example = "노이즈 캔슬링 기능을 갖춘 최신형 에어팟 프로 2세대입니다.")
    private final String productDescription;

    @Schema(description = "경매 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-25T09:00:00")
    private final LocalDateTime startTime;

    @Schema(description = "경매 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-26T09:00:00")
    private final LocalDateTime endTime;

    @Schema(description = "경매 시작 가격", example = "10000")
    private final Long startingBid;

    @Schema(description = "입찰 단위 (입찰 시 증가 금액)", example = "1000")
    private final Long bidStep;

    @Schema(description = "현재 입찰가", example = "35000")
    private final Long currentBid;

    @Schema(description = "로그인 사용자가 해당 경매에 참여했는지 여부", example = "true")
    private final Boolean myParticipated;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
    private final AuctionStatus status;

    @Schema(description = "사용자가 지금 이 경매에 입찰 가능한 상태인지 여부(로그인/비로그인)", example = "true")
    private final Boolean canBid;

    private AuctionReadResponse(Long id, String imageUrl, String productName, String productDescription, LocalDateTime startTime,
                                LocalDateTime endTime, Long startingBid, Long bidStep, Long currentBid, Boolean myParticipated, AuctionStatus status, Boolean canBid) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.productName = productName;
        this.productDescription = productDescription;
        this.startTime = startTime;
        this.endTime = endTime;
        this.startingBid = startingBid;
        this.bidStep = bidStep;
        this.currentBid = currentBid;
        this.myParticipated = myParticipated;
        this.status = status;
        this.canBid = canBid;
    }

    public static AuctionReadResponse from(Auction auction, Product product, String imageUrl, Long currentBid, Boolean myParticipated, Boolean canBid) {
        return new AuctionReadResponse(
                auction.getId(),
                imageUrl,
                product.getName(),
                product.getDescription(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStartingBid(),
                auction.getBidStep(),
                currentBid,
                myParticipated,
                auction.getDynamicStatus(),
                canBid);
    }
}
