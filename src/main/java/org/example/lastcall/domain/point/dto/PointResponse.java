package org.example.lastcall.domain.point.dto;

import lombok.Getter;

@Getter
public class PointResponse {

    private Long availablePoint;
    private Long depositPoint;
    private Long settlementPoint;
    private Long totalAmount;
}
