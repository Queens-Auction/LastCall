package org.example.lastcall.domain.point.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "포인트 조회 및 충전 응답 DTO")
@Getter
@AllArgsConstructor
public class PointResponse {
    @Schema(description = "사용자 ID", example = "101")
    private Long userId;

    @Schema(description = "포인트 ID", example = "5001")
    private Long pointId;

    @Schema(description = "현재 사용 가능한 포인트", example = "15000")
    private Long availablePoint;

    @Schema(description = "예치 포인트 (입찰 참여 시 예치된 금액)", example = "30000")
    private Long depositPoint;

    @Schema(description = "정산 중인 포인트", example = "5000")
    private Long settlementPoint;
}
