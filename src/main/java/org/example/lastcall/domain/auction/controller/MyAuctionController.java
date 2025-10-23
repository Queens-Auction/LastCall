package org.example.lastcall.domain.auction.controller;

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
import org.example.lastcall.domain.auth.model.AuthUser;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 내가 참여한 경매 전체 조회 //
    @GetMapping("/participated")
    public ResponseEntity<ApiResponse<PageResponse<MyParticipatedResponse>>> getMyParticipatedAuctions(@Auth AuthUser authUser,
                                                                                                       Pageable pageable) {
        PageResponse<MyParticipatedResponse> pageResponse = myAuctionService.getMyParticipatedAuctions(authUser.userId(), pageable);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 참여한 경매 목록이 조회되었습니다.", pageResponse)
        );
    }

    // 내가 참여한 경매 단건 조회 //
    @GetMapping("/participated/{auctionId}")
    public ResponseEntity<ApiResponse<MyParticipatedResponse>> getMyParticipatedDetailAuction(@Auth AuthUser authUser,
                                                                                              @PathVariable Long auctionId) {
        MyParticipatedResponse response = myAuctionService.getMyParticipatedDetailAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내가 참여한 경매 중 해당 경매가 조회되었습니다.", response)
        );
    }

    // 내 경매 수정 //
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
    @DeleteMapping("{auctionId}")
    public ResponseEntity<ApiResponse<Void>> deleteAuction(@Auth AuthUser authUser,
                                                           @PathVariable Long auctionId) {
        myAuctionService.deleteAuction(authUser.userId(), auctionId);

        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success("내 경매가 삭제되었습니다.")
        );
    }
}
