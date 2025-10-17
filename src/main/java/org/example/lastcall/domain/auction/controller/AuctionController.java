package org.example.lastcall.domain.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.service.AuctionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/actions")
public class AuctionController {
    private final AuctionService auctionService;

    // 경매 등록 //
    @PostMapping
    public ResponseEntity<ApiResponse<AuctionResponse>> createAuction(Long userId,
                                                                      // 시큐리티 적용 후, @AuthenticationPrincipal AuthUser authUser 로 변경 예정
                                                                      @Valid @RequestBody AuctionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("경매가 등록되었습니다.", auctionService.createAuction(userId, request))
        );
    }
}
