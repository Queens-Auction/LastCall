package org.example.lastcall.domain.bid.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.lastcall.domain.bid.entity.Bid;

import java.time.LocalDateTime;

@Schema(description = "입찰 등록 응답 DTO")
@Getter
@AllArgsConstructor
public class BidResponse {
    @Schema(description = "입찰 ID", example = "1001")
    private final Long id;

    @Schema(description = "경매 ID", example = "501")
    private final Long auctionId;

    @Schema(description = "사용자 ID", example = "2001")
    private final Long userId;

    @Schema(description = "입찰 금액", example = "155000")
    private final Long bidAmount;

    @Schema(description = "입찰 생성 시각", example = "2025-10-24T15:45:00")
    private final LocalDateTime createdAt;

    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getUser().getId(),
                bid.getBidAmount(),
                bid.getCreatedAt());
    }
}
