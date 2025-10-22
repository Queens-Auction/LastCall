package org.example.lastcall.domain.auction.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.service.MyAuctionService;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my/auctions")
public class MyAuctionController {

    private final MyAuctionService myAuctionService;

    // 내가 판매한 경매 목록 조회 //
    @GetMapping("/selling")
    public ResponseEntity<ApiResponse<PageResponse<MySellingResponse>>> getMySellingAuctions(@Auth AuthUser authUser,
                                                                                             Pageable pageable) {
        PageResponse<MySellingResponse> pageResponse = myAuctionService.getMySellingAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 판매한 경매 상세 조회 //
    @GetMapping("/selling/{auctionId}")
    public ResponseEntity<ApiResponse<MySellingResponse>> getMySellingDetailAuctions(@Auth AuthUser authUser,
                                                                                     @PathVariable Long auctionId) {
        MySellingResponse response = myAuctionService.getMySellingDetailAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 중 해당 경매가 조회되었습니다.", response)
        );
    }
}
