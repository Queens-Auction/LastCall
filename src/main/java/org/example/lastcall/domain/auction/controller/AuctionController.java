package org.example.lastcall.domain.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.service.AuctionService;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions") // 오타 수정
public class AuctionController {
    private final AuctionService auctionService;

    // 경매 등록 //
    @PostMapping
    public ResponseEntity<ApiResponse<AuctionCreateResponse>> createAuction(@Auth AuthUser authUser,
                                                                            @Valid @RequestBody AuctionCreateRequest request) {
        AuctionCreateResponse response = auctionService.createAuction(authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("경매가 등록되었습니다.", response)
        );
    }
}
