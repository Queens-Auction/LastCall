package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;

import java.time.LocalDateTime;

@Schema(description = "내 경매 생성/수정/삭제 응답 DTO")
@Getter
public class AuctionResponse {
    @Schema(description = "경매 ID", example = "101")
    private final Long id;

    @Schema(description = "상품 ID", example = "55")
    private final Long productId;

    @Schema(description = "등록한 사용자 ID", example = "7")
    private final Long userId;

    @Schema(description = "경매 시작 가격", example = "10000")
    private final Long startingBid;

    @Schema(description = "입찰 단위 (입찰 시 증가 금액)", example = "1000")
    private final Long bidStep;

    @Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
    private final LocalDateTime startTime;

    @Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
    private final LocalDateTime endTime;

    @Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "SCHEDULED")
    private final AuctionStatus status;

    @Schema(description = "경매 등록 시각", example = "2025-10-24T21:10:00")
    private final LocalDateTime createdAt;

    @Schema(description = "경매 수정 시각", example = "2025-10-27T21:10:00")
    private final LocalDateTime modifiedAt;

    private AuctionResponse(Long id, Long productId, Long userId, Long startingBid, Long bidStep, LocalDateTime startTime,
                            LocalDateTime endTime, AuctionStatus status, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.productId = productId;
        this.userId = userId;
        this.startingBid = startingBid;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    // 경매 등록 응답용
    public static AuctionResponse fromCreate(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getProduct().getId(),
                auction.getUser().getId(),
                auction.getStartingBid(),
                auction.getBidStep(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getDynamicStatus(),
                auction.getCreatedAt(),
                null
        );
    }

    // 경매 수정 응답용
    public static AuctionResponse fromUpdate(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getProduct().getId(),
                auction.getUser().getId(),
                auction.getStartingBid(),
                auction.getBidStep(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getDynamicStatus(),
                auction.getCreatedAt(),
                auction.getModifiedAt()
        );
    }
}
