package org.example.lastcall.domain.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.service.AuctionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ApiResponse<AuctionCreateResponse>> createAuction(@RequestHeader("userId") Long userId,
                                                                            // 시큐리티 적용 후, @AuthenticationPrincipal AuthUser authUser 로 변경 예정
                                                                            @Valid @RequestBody AuctionCreateRequest request) {
        AuctionCreateResponse response = auctionService.createAuction(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("경매가 등록되었습니다.", response)
        );
    }

    // 경매 전체 조회 //
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuctionReadAllResponse>>> readAllAuctions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<AuctionReadAllResponse> sliceResponse = auctionService.readAllAuctions(pageable);
        ApiResponse<PageResponse<AuctionReadAllResponse>> apiResponse = ApiResponse.success("경매를 전체 조회했습니다.", sliceResponse);

        return ResponseEntity.ok(apiResponse);
    }
}
