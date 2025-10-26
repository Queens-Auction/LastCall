package org.example.lastcall.domain.auction.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.service.MyAuctionService;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "내 경매 API", description = "내가 등록한(판매한) 또는 참여한 경매의 조회, 수정, 삭제 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my/auctions")
public class MyAuctionController {

    private final MyAuctionService myAuctionService;

    // 내가 판매한 경매 목록 조회 //
    @Operation(
            summary = "내가 판매한 경매 목록 조회",
            description = "로그인한 사용자가 자신이 등록한(판매 중이거나 종료된) 모든 경매 목록을 조회합니다. " +
                    "페이징 처리가 적용되어 있으며, 최신순으로 정렬됩니다."
    )
    @GetMapping("/selling")
    public ResponseEntity<ApiResponse<PageResponse<MySellingResponse>>> getMySellingAuctions(@Auth AuthUser authUser,
                                                                                             Pageable pageable) {
        PageResponse<MySellingResponse> pageResponse = myAuctionService.getMySellingAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 판매한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 판매한 경매 상세 조회 //
    @Operation(
            summary = "내가 판매한 경매 상세 조회",
            description = "로그인한 사용자가 자신이 등록한 경매 중 특정 경매의 상세 정보를 조회합니다."
    )
    @GetMapping("/selling/{auctionId}")
    public ResponseEntity<ApiResponse<MySellingResponse>> getMySellingDetailAuctions(@Auth AuthUser authUser,
                                                                                     @PathVariable Long auctionId) {
        MySellingResponse response = myAuctionService.getMySellingDetailAuction(authUser.userId(), auctionId);

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
    @GetMapping("/participated")
    public ResponseEntity<ApiResponse<PageResponse<MyParticipatedResponse>>> getMyParticipatedAuctions(@Auth AuthUser authUser,
                                                                                                       Pageable pageable) {
        PageResponse<MyParticipatedResponse> pageResponse = myAuctionService.getMyParticipatedAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 참여한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 참여한 경매 단건 조회 //
    @Operation(
            summary = "내가 참여한 경매 상세 조회",
            description = "로그인한 사용자가 입찰에 참여한 특정 경매의 상세 정보를 조회합니다."
    )
    @GetMapping("/participated/{auctionId}")
    public ResponseEntity<ApiResponse<MyParticipatedResponse>> getMyParticipatedDetailAuction(@Auth AuthUser authUser,
                                                                                              @PathVariable Long auctionId) {
        MyParticipatedResponse response = myAuctionService.getMyParticipatedDetailAuction(authUser.userId(), auctionId);

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
    @PatchMapping("{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> updateAuction(@Auth AuthUser authUser,
                                                                      @PathVariable Long auctionId,
                                                                      @Valid @RequestBody AuctionUpdateRequest request) {
        AuctionResponse response = myAuctionService.updateAuction(authUser.userId(), auctionId, request);

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
    @DeleteMapping("{auctionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuction(@Auth AuthUser authUser,
                                                           @PathVariable Long auctionId) {
        myAuctionService.deleteAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내 경매가 삭제되었습니다.")
        );
    }
}
