package org.example.lastcall.domain.point.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.point.dto.CreatePointRequest;
import org.example.lastcall.domain.point.dto.PointResponse;
import org.example.lastcall.domain.point.service.PointService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PointController {

    private final PointService pointService;

    // 포인트 충전
    @PostMapping("/users/{userId}/points/earn")
    public ResponseEntity<PointResponse> createPoint(
            @PathVariable Long userId,
            @RequestBody @Valid CreatePointRequest request) {

        PointResponse pointResponse = pointService.createPoint(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(pointResponse);
    }
}
