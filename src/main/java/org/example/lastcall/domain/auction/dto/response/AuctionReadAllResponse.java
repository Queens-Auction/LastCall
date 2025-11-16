package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;

@Schema(description = "경매 전체 조회 응답 DTO")
@Getter
public class AuctionReadAllResponse {
    @Schema(description = "경매 ID", example = "101")
    private final Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private final String imageUrl;

    @Schema(description = "상품 이름", example = "에어팟 프로 2세대")
    private final String productName;

    @Schema(description = "현재 참여자 수", example = "8")
    private final Long participantCount;

    public AuctionReadAllResponse(Long id, String imageUrl, String productName, Long participantCount) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.productName = productName;
        this.participantCount = (participantCount != null) ? participantCount : 0L;
    }

    public static AuctionReadAllResponse from(Auction auction, String imageUrl, Long participantCount) {
        return new AuctionReadAllResponse(
                auction.getId(),
                imageUrl,
                auction.getProduct().getName(),
                participantCount);
    }
}
