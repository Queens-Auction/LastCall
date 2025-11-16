package org.example.lastcall.domain.auction.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "경매 상태를 나타내는 Enum")
@Getter
public enum AuctionStatus {
    @Schema(description = "경매가 아직 시작되지 않은 상태")
    SCHEDULED("경매 대기"),

    @Schema(description = "경매가 진행 중인 상태")
    ONGOING("경매 진행 중"),

    @Schema(description = "경매가 종료(낙찰)된 상태")
    CLOSED("경매 종료(낙찰)"),

    @Schema(description = "경매가 종료(유찰)된 상태")
    CLOSED_FAILED("경매 종료(유찰)"),

    @Schema(description = "경매가 삭제된 상태")
    DELETED("삭제된 경매");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }
}