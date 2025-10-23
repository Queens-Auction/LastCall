package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Getter
@Builder
public class MySellingResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private String productDescription;
    private Long currentBid;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 정적 팩토리 메서드 (from)
    public static MySellingResponse from(Auction auction, Product product, String imageUrl, Long currentBid) {
        return MySellingResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .currentBid(currentBid)
                .status(auction.getDynamicStatus()) // 조회 시점 상태 계산
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .build();
    }
}
