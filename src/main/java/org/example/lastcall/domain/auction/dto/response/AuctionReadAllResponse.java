package org.example.lastcall.domain.auction.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;

@Schema(description = "경매 전체 조회 응답 DTO")
@Getter
@Builder
// DTO는 DB나 JPA 와 상관없기 때문에 클래스 단위로 붙이는 게 가독성 + 편의성 모두 좋음
public class AuctionReadAllResponse {
    @Schema(description = "경매 ID", example = "101")
    private Long id;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.lastcall.com/images/auction_101.jpg")
    private String imageUrl;

    @Schema(description = "상품 이름", example = "에어팟 프로 2세대")
    private String productName;

    @Schema(description = "현재 참여자 수", example = "8")
    private int participantCount;

    // 정적 팩토리 메서드 (from)
    public static AuctionReadAllResponse from(Auction auction, String imageUrl, int participantCount) {
        return AuctionReadAllResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(auction.getProduct().getName())
                .participantCount(participantCount)
                .build();
    }
}
