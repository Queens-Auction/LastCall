package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;

import java.time.LocalDateTime;

@Schema(description = "내 경매 생성/수정/삭제 응답 DTO")
@Getter
@Builder
public class AuctionResponse {
    @Schema(description = "경매 ID", example = "101")
    private Long auctionId;

    @Schema(description = "상품 ID", example = "55")
    private Long productId;

    @Schema(description = "등록한 사용자 ID", example = "7")
    private Long userId;

    @Schema(description = "경매 시작 가격", example = "10000")
    private Long startingBid;

    @Schema(description = "입찰 단위 (입찰 시 증가 금액)", example = "1000")
    private Long bidStep;

    @Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
    private LocalDateTime startTime;

    @Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
    private LocalDateTime endTime;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "SCHEDULED")
    private AuctionStatus status;

    @Schema(description = "경매 등록 시각", example = "2025-10-24T21:10:00")
    private LocalDateTime createdAt;

    @Schema(description = "경매 수정 시각", example = "2025-10-27T21:10:00")
    private LocalDateTime modifiedAt;

    // 정적 팩토리 메서드 (from) - 경매 등록 응답용
    public static AuctionResponse fromCreate(Auction auction) {
        return AuctionResponse.builder()
                .auctionId(auction.getId())
                .productId(auction.getProduct().getId())
                .userId(auction.getUser().getId())
                .startingBid(auction.getStartingBid())
                .bidStep(auction.getBidStep())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .build();
    }

    // 정적 팩토리 메서드 (from) - 경매 수정 응답용
    public static AuctionResponse fromUpdate(Auction auction) {
        return AuctionResponse.builder()
                .auctionId(auction.getId())
                .productId(auction.getProduct().getId())
                .userId(auction.getUser().getId())
                .startingBid(auction.getStartingBid())
                .bidStep(auction.getBidStep())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .modifiedAt(auction.getModifiedAt())
                .build();
    }
}
