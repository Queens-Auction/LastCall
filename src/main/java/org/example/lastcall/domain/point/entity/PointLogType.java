package org.example.lastcall.domain.point.entity;

import lombok.Getter;

@Getter
public enum PointLogType {

    EARN("포인트 결제로 인한 충전"),
    WITHDRAW("경매 유찰된 포인트"),
    DEPOSIT("경매 참여 중인 포인트"),
    SETTLEMENT("경매 낙찰 확정 후 결제된 포인트"),
    REFUND("경매 취소 & 환불로 인한 포인트 리펀");

    private String description;

    PointLogType(String description) {
        this.description = description;
    }
}
