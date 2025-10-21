package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionReadResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private String productDescription;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long startingBid;
    private Long bidStep;
    private Boolean myParticipated;
    private AuctionStatus status;

    // 정적 팩토리 메서드 (from)
    public static AuctionReadResponse from(
            Auction auction,
            Product product,
            String imageUrl,
            boolean myParticipated
    ) {
        return AuctionReadResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(product.getName())
                .productDescription(product.getDescription())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .startingBid(auction.getStartingBid())
                .bidStep(auction.getBidStep())
                .myParticipated(myParticipated)
                .status(auction.getStatus())
                .build();
    }
}
