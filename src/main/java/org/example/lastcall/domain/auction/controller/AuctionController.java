package org.example.lastcall.domain.auction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.*;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.example.lastcall.domain.auction.service.query.AuctionQueryService;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "경매 API", description = "경매 등록, 전체 조회, 상세 조회 기능 제공")
// -> Swagger 그룹 이름 및 설명
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions")
public class AuctionController {
    private final AuctionQueryService auctionQueryService;
    private final AuctionCommandService auctionCommandService;

    // 경매 등록 //
    @Operation(
            summary = "경매 등록",
            description = "로그인한 사용자가 새로운 경매를 등록합니다. " +
                    "요청 본문에는 상품 정보, 시작가, 마감 시간 등이 포함됩니다."
    )
    @PostMapping("/me/{productId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> createAuction(@PathVariable("productId") Long productId,
                                                                      @Auth AuthUser authUser,
                                                                      @Valid @RequestBody AuctionCreateRequest request) {
        AuctionResponse response = auctionCommandService.createAuction(productId, authUser.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("경매가 등록되었습니다.", response)
        );
    }

    // 경매 전체 조회 //
    @Operation(
            summary = "경매 전체 조회",
            description = "모든 경매 목록을 최신순으로 조회합니다. " +
                    "카테고리별 필터링도 가능합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuctionReadAllResponse>>> getAllAuctions(
            @RequestParam(required = false) Category category,
            // 기본 정렬값 createdAt, 보조 정렬값 id
            // - 보조가 없으면 MySQL 이 비슷하거나 동시간대 정렬 구분 못함
            @PageableDefault(size = 10, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<AuctionReadAllResponse> pageResponse = auctionQueryService.getAllAuctions(category, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("경매가 전체 조회되었습니다.", pageResponse)
        );
    }

    // 경매 단건 상세 조회 //
    // required = false : 로그인 안 한 사용자도 접근 가능 (null 가능)
    @Operation(
            summary = "경매 상세 조회",
            description = "경매 ID를 이용해 단일 경매의 상세 정보를 조회합니다. " +
                    "비로그인 사용자도 접근 가능합니다."
    )
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionReadResponse>> getAuction(
            @PathVariable("auctionId") Long auctionId,
            @RequestHeader(value = "userId", required = false) Long userId   // 시큐리티 적용 후, @AuthenticationPrincipal AuthUser authUser 로 변경 예정
    ) {
        AuctionReadResponse response = auctionQueryService.getAuction(auctionId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("해당 경매가 조회되었습니다.", response)
        );
    }

    // 내가 판매한 경매 목록 조회 //
    @Operation(
            summary = "내가 판매한 경매 목록 조회",
            description = "로그인한 사용자가 자신이 등록한(판매 중이거나 종료된) 모든 경매 목록을 조회합니다. " +
                    "페이징 처리가 적용되어 있으며, 최신순으로 정렬됩니다."
    )
    @GetMapping("/me/selling")
    public ResponseEntity<ApiResponse<PageResponse<MySellingResponse>>> getMySellingAuctions(
            @Auth AuthUser authUser,
            @PageableDefault(size = 2, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MySellingResponse> pageResponse = auctionQueryService.getMySellingAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 판매한 경매 상세 조회 //
    @Operation(
            summary = "내가 판매한 경매 상세 조회",
            description = "로그인한 사용자가 자신이 등록한 경매 중 특정 경매의 상세 정보를 조회합니다."
    )
    @GetMapping("/me/selling/{auctionId}")
    public ResponseEntity<ApiResponse<MySellingResponse>> getMySellingDetailAuctions(@Auth AuthUser authUser,
                                                                                     @PathVariable Long auctionId) {
        MySellingResponse response = auctionQueryService.getMySellingDetailAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 중 해당 경매가 조회되었습니다.", response)
        );
    }

    // 내가 참여한 경매 전체 조회 //
    @Operation(
            summary = "내가 참여한 경매 목록 조회",
            description = "로그인한 사용자가 입찰에 참여했던 모든 경매 목록을 조회합니다. " +
                    "페이징 처리가 적용되어 있으며, 최신순으로 정렬됩니다."
    )
    @GetMapping("/me/participated")
    public ResponseEntity<ApiResponse<PageResponse<MyParticipatedResponse>>> getMyParticipatedAuctions(
            @Auth AuthUser authUser,
            @PageableDefault(size = 2, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<MyParticipatedResponse> pageResponse = auctionQueryService.getMyParticipatedAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 참여한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 참여한 경매 단건 조회 //
    @Operation(
            summary = "내가 참여한 경매 상세 조회",
            description = "로그인한 사용자가 입찰에 참여한 특정 경매의 상세 정보를 조회합니다."
    )
    @GetMapping("/me/participated/{auctionId}")
    public ResponseEntity<ApiResponse<MyParticipatedResponse>> getMyParticipatedDetailAuction(@Auth AuthUser authUser,
                                                                                              @PathVariable Long auctionId) {
        MyParticipatedResponse response = auctionQueryService.getMyParticipatedDetailAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 참여한 경매 중 해당 경매가 조회되었습니다.", response)
        );
    }

    // 내 경매 수정 //
    @Operation(
            summary = "내 경매 수정",
            description = "로그인한 사용자가 자신이 등록한 경매의 정보를 수정합니다. " +
                    "시작가, 입찰 단위, 시작/종료 시간을 변경할 수 있습니다."
    )
    @PatchMapping("/me/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> updateAuction(@Auth AuthUser authUser,
                                                                      @PathVariable Long auctionId,
                                                                      @Valid @RequestBody AuctionUpdateRequest request) {
        AuctionResponse response = auctionCommandService.updateAuction(authUser.userId(), auctionId, request);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내 경매가 수정되었습니다.", response)
        );
    }

    // 내 경매 삭제 //
    @Operation(
            summary = "내 경매 삭제",
            description = "로그인한 사용자가 자신이 등록한 경매를 삭제합니다. " +
                    "해당 경매가 이미 시작되었거나 종료된 경우 삭제가 제한될 수 있습니다."
    )
    @DeleteMapping("/me/{auctionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuction(@Auth AuthUser authUser,
                                                           @PathVariable Long auctionId) {
        auctionCommandService.deleteAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내 경매가 삭제되었습니다.")
        );
    }
}
