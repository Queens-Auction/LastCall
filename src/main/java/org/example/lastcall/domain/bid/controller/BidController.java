package org.example.lastcall.domain.bid.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.dto.request.BidCreateRequest;
import org.example.lastcall.domain.bid.dto.response.BidCreateResponse;
import org.example.lastcall.domain.bid.dto.response.BidGetAllResponse;
import org.example.lastcall.domain.bid.service.command.BidCommandService;
import org.example.lastcall.domain.bid.service.query.BidQueryService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "입찰(Bid) API", description = "경매 입찰 생성 및 조회 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController {
    private final BidQueryService bidQueryService;
    private final BidCommandService bidCommandService;

    @Operation(
            summary = "입찰 등록",
            description = "로그인한 사용자가 해당 경매에 입찰을 등록합니다. " +
                    "경매 진행 중일 경우에만 가능하며, 이전 최고가보다 높은 금액으로 자동 계산됩니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<BidCreateResponse>> createBid(
            @PathVariable Long auctionId,
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody BidCreateRequest request) {
        BidCreateResponse bid = bidCommandService.createBid(auctionId, authUser, request.getNextBidAmount());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("입찰이 완료되었습니다.", bid));
    }

    @Operation(
            summary = "경매별 입찰 내역 조회 (최신순)",
            description = "특정 경매에 대한 전체 입찰 내역을 최신순으로 조회합니다. " +
                    "페이징이 적용되며, 기본 5개씩 반환됩니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BidGetAllResponse>>> getAllBids(
            @PathVariable Long auctionId,
            @ParameterObject
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<BidGetAllResponse> bids = bidQueryService.getAllBids(auctionId, pageable);

        return ResponseEntity.ok(ApiResponse.success("해당 경매의 입찰 내역을 조회합니다.", bids));
    }
}
