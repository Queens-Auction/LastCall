package org.example.lastcall.domain.auction.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "경매 상태를 나타내는 Enum")
@Getter
@RequiredArgsConstructor
public enum AuctionStatus {
    @Schema(description = "경매가 아직 시작되지 않은 상태")
    SCHEDULED,

    @Schema(description = "경매가 진행 중인 상태")
    ONGOING,

    @Schema(description = "경매가 종료된 상태")
    CLOSED,

    @Schema(description = "경매가 삭제된 상태")
    DELETED
}