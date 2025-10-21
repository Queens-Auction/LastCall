package org.example.lastcall.domain.bid.controller;

import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.service.BidService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController {
	private final BidService bidService;

	// 입찰 등록
	@PostMapping
	public ResponseEntity<ApiResponse<BidResponse>> createBid(@PathVariable Long auctionId, Long userId) {
		BidResponse bid = bidService.createBid(auctionId, userId);

		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("입찰이 완료되었습니다.", bid));
	}
}
