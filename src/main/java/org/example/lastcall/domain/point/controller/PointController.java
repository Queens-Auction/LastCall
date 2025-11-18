package org.example.lastcall.domain.point.controller;

import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.point.dto.request.PointCreateRequest;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.service.command.PointCommandService;
import org.example.lastcall.domain.point.service.query.PointQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "포인트(Point) API", description = "사용자 포인트 충전 및 조회 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/points")
public class PointController {
	private final PointCommandService pointCommandService;
	private final PointQueryService pointQueryService;

	// 포인트 충전
	@Operation(
		summary = "포인트 충전",
		description = "로그인한 사용자가 지정된 금액만큼 포인트를 충전합니다. " +
			"요청 시 본인 인증이 필요하며, 충전 금액은 0보다 커야 합니다."
	)
	@PostMapping("/earn")
	public ResponseEntity<ApiResponse<PointResponse>> createPoint(
		@AuthenticationPrincipal AuthUser authUser,
		@RequestBody @Valid PointCreateRequest request) {
		PointResponse pointResponse = pointCommandService.createPoint(authUser, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("포인트 충전이 완료되었습니다.", pointResponse));
	}

	// 유저 포인트 조회
	@Operation(
		summary = "포인트 조회",
		description = "로그인한 사용자의 현재 포인트 잔액을 조회합니다."
	)
	@GetMapping
	public ResponseEntity<ApiResponse<PointResponse>> getUserPoint(@AuthenticationPrincipal AuthUser authUser) {
		PointResponse pointResponse = pointQueryService.getUserPoint(authUser);

		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("포인트 조회가 완료되었습니다.", pointResponse));
	}
}
