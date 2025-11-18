package org.example.lastcall.domain.auction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "내 경매 수정 요청 DTO")
@Getter
public class AuctionUpdateRequest {
    @Schema(description = "경매 수정 요청 DTO")
    @NotNull(message = "경매 시작 가격은 필수 입력값입니다.")
    @Positive(message = "시작가는 0보다 커야 합니다.")
    private Long startingBid;

    @Schema(description = "수정할 경매 입찰 단위 (입찰 최소 증가 금액)", example = "1000")
    @NotNull(message = "경매 입찰 단위는 필수 입력값입니다.")
    @Positive(message = "입찰 단위는 0보다 커야 합니다.")
    private Long bidStep;

    @Schema(description = "수정된 경매 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-26T10:00:00")
    @NotNull(message = "경매 시작일은 필수 입력값입니다.")
    @Future(message = "시작일은 현재 시각 이후여야 합니다.")
    private LocalDateTime startTime;

    @Schema(description = "수정된 경매 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", example = "2025-10-27T10:00:00")
    @NotNull(message = "경매 종료일은 필수 입력값입니다.")
    private LocalDateTime endTime;
}
