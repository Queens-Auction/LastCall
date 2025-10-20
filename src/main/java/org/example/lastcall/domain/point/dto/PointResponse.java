package org.example.lastcall.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PointResponse {

    private Long userId;
    private Long availablePoint;
    private Long depositPoint;
    private Long settlementPoint;
}
