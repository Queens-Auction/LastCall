package org.example.lastcall.domain.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadResponse;
import org.example.lastcall.domain.auction.service.AuctionService;
import org.example.lastcall.domain.product.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // 경매 전체 조회 //
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuctionReadAllResponse>>> readAllAuctions(
            @RequestParam(required = false) Category category,
            // 기본 정렬값 createdAt, 보조 정렬값 id
            // - 보조가 없으면 MySQL 이 비슷하거나 동시간대 정렬 구분 못함
            @PageableDefault(size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<AuctionReadAllResponse> pageResponse = auctionService.readAllAuctions(category, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("경매가 전체 조회되었습니다.", pageResponse)
        );
    }

    // 경매 단건 상세 조회 //
    // required = false : 로그인 안 한 사용자도 접근 가능 (null 가능)
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionReadResponse>> readAuction(
            @PathVariable("auctionId") Long auctionId,
            @RequestHeader(value = "userId", required = false) Long userId   // 시큐리티 적용 후, @AuthenticationPrincipal AuthUser authUser 로 변경 예정
    ) {
        AuctionReadResponse response = auctionService.readAuction(auctionId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("해당 경매가 조회되었습니다.", response)
        );
    }
}
