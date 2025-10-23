package org.example.lastcall.domain.point.dto;

import lombok.Getter;
import org.example.lastcall.domain.point.entity.PointLogType;

@Getter
public class CreatePointRequest {

    private Long userId;              // 사용자 ID
    private Long bidId;
    private PointLogType type;        // 포인트 변동 유형 (EARN, USE, ect...)
    private String description;       // 변동 사유
    private Long incomePoint;         // 입금된 포인트
}
