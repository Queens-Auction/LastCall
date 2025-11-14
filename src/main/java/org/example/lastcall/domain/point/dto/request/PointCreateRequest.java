package org.example.lastcall.domain.point.dto.request;

import org.example.lastcall.domain.point.enums.PointLogType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "포인트 충전 요청 DTO")
@Getter
public class PointCreateRequest {
	@Schema(description = "사용자 ID", example = "101")
	private Long userId;

	@Schema(description = "입찰 ID (입찰 관련 포인트 적립 시 사용)", example = "202")
	private Long bidId;

	@Schema(description = "입찰 ID (입찰 관련 포인트 적립 시 사용)", example = "202")
	private PointLogType type;

	@Schema(description = "포인트 변동 사유", example = "경매 낙찰 리워드 적립")
	private String description;

	@Schema(description = "입금된 포인트 금액", example = "5000")
	private Long incomePoint;
}
