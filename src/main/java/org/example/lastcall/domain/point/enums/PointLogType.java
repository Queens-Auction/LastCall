package org.example.lastcall.domain.point.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "포인트 변동 유형 (입찰/환불/정산 등)")
@Getter
public enum PointLogType {
    @Schema(description = "포인트 결제로 인한 충전")
    EARN("포인트 결제로 인한 충전"),

    @Schema(description = "경매 유찰 시 반환된 포인트")
    WITHDRAW("경매 유찰된 포인트"), // 경매가 유찰돼 낙찰자 자체가 없을 때 반환되는 포인트

    @Schema(description = "경매 참여 중 예치된 포인트")
    DEPOSIT("경매 참여 중인 포인트"),

    @Schema(description = "입찰 금액 증가로 인한 추가 예치 처리")
    ADDITIONAL_DEPOSIT("입찰 금액 증가로 인한 추가 예치 처리"),

    @Schema(description = "경매 낙찰 확정 후 결제된 포인트")
    SETTLEMENT("경매 낙찰 확정 후 결제된 포인트"),

    @Schema(description = "경매 취소 또는 환불로 인한 포인트 리펀")
    REFUND("경매 취소 & 환불로 인한 포인트 리펀"),

    @Schema(description = "입찰 실패로 인한 예치금 반환")
    DEPOSIT_TO_AVAILABLE("입찰 실패로 인한 예치금 반환");

    private String description;

    PointLogType(String description) {
        this.description = description;
    }
}
