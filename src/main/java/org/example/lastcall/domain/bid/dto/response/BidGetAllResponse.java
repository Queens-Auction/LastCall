package org.example.lastcall.domain.bid.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.lastcall.domain.bid.entity.Bid;

import java.time.LocalDateTime;

@Schema(description = "입찰 내역 조회 응답 DTO")
@Getter
@AllArgsConstructor
public class BidGetAllResponse {
    @Schema(description = "입찰 ID", example = "1001")
    private final Long id;

    @Schema(description = "경매 ID", example = "501")
    private final Long auctionId;

    @Schema(description = "입찰자 닉네임", example = "lastcaller")
    private final String nickname;

    @Schema(description = "입찰 금액", example = "150000")
    private final Long bidAmount;

    @Schema(description = "입찰 시각 (생성 일시)", example = "2025-10-24T15:30:00")
    private final LocalDateTime createdAt;

    public static BidGetAllResponse from(Bid bid) {
        return new BidGetAllResponse(
                bid.getId(),
                bid.getAuction().getId(),
                bid.getUser().getNickname(),
                bid.getBidAmount(),
                bid.getCreatedAt());
    }
}
