package org.example.lastcall.domain.bid.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Schema(description = "입찰 요청 DTO")
@Getter
public class BidRequest {
    @Schema(description = "프론트에서 계산된 다음 입찰 금액 (현재 입찰가 + 입찰단위)", example = "15000")
    @NotNull(message = "입찰 금액은 필수 입력값입니다.")
    @Positive(message = "입찰 금액은 0보다 커야 합니다.")
    private Long nextBidAmount;
}
