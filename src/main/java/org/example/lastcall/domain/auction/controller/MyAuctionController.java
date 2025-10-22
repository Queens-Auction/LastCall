package org.example.lastcall.domain.auction.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingAuctionResponse;
import org.example.lastcall.domain.auction.service.MyAuctionService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my/auctions")
public class MyAuctionController {

    private final MyAuctionService myAuctionService;

    // 내가 판매한 경매 목록 조회 //
    @GetMapping("/selling")
    public ResponseEntity<ApiResponse<PageResponse<MySellingAuctionResponse>>> getMySellingAuctions(
            //@AuthenticationPrincipal AuthUser authUser,
            // AuthUser 적용되면 email -> user.getId()
            Pageable pageable) {
        Long testUserId = 5L; // DB에 실제 존재하는 user_id (임시) -> authUser 적용 후 삭제하기
        PageResponse<MySellingAuctionResponse> pageResponse = myAuctionService.getMySellingAuctions(testUserId, pageable);
        // AuthUser 적용되면 email -> user.getId()로 수정 예정
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }
}
