package org.example.lastcall.domain.auction.dto.response;

import java.time.LocalDateTime;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "내가 판매한 경매 조회 응답 DTO")
@Getter
public class MySellingResponse {
	@Schema(description = "경매 ID", example = "101")
	private final Long id;

	@Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
	private final String imageUrl;

	@Schema(description = "상품 이름", example = "아이폰 15 프로 256GB")
	private final String productName;

	@Schema(description = "상품 상세 설명", example = "미개봉 새 제품, 그레이 색상 모델입니다.")
	private final String productDescription;

	@Schema(description = "현재 최고 입찰가", example = "1250000")
	private final Long currentBid;

	@Schema(description = "경매 상태 (예: SCHEDULED, ONGOING, CLOSED)", example = "ONGOING")
	private final AuctionStatus status;

	@Schema(description = "경매 시작 시간", example = "2025-10-25T09:00:00")
	private final LocalDateTime startTime;

	@Schema(description = "경매 종료 시간", example = "2025-10-26T09:00:00")
	private final LocalDateTime endTime;

	public MySellingResponse(Long id, String imageUrl, String productName, String productDescription,
						     Long currentBid, AuctionStatus status, LocalDateTime startTime, LocalDateTime endTime) {
		this.id = id;
		this.imageUrl = imageUrl;
		this.productName = productName;
		this.productDescription = productDescription;
		this.currentBid = currentBid;
		this.status = status;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public static MySellingResponse from(Auction auction, Product product, String imageUrl, Long currentBid) {
		return new MySellingResponse(
			auction.getId(),
			imageUrl,
			product.getName(),
			product.getDescription(),
			currentBid,
			auction.getDynamicStatus(),
			auction.getStartTime(),
			auction.getEndTime());
	}
}
