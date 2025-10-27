package org.example.lastcall.domain.point.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.point.dto.request.CreatePointRequest;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.service.PointService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "포인트(Point) API", description = "사용자 포인트 충전 및 조회 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/points")
public class PointController {

    private final PointService pointService;

    // 포인트 충전
    @Operation(
            summary = "포인트 충전",
            description = "로그인한 사용자가 지정된 금액만큼 포인트를 충전합니다. " +
                    "요청 시 본인 인증이 필요하며, 충전 금액은 0보다 커야 합니다."
    )
    @PostMapping("/earn")
    public ResponseEntity<PointResponse> createPoint(
            @Auth AuthUser authUser,
            @RequestBody @Valid CreatePointRequest request) {

        PointResponse pointResponse = pointService.createPoint(authUser, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(pointResponse);
    }

    // 유저 포인트 조회
    @Operation(
            summary = "포인트 조회",
            description = "로그인한 사용자의 현재 포인트 잔액을 조회합니다."
    )
    @GetMapping
    public ResponseEntity<PointResponse> getUserPoint(@Auth AuthUser authUser) {

        PointResponse pointResponse = pointService.getUserPoint(authUser);

        return ResponseEntity.status(HttpStatus.OK).body(pointResponse);
    }
}
