package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionCreateResponse {
    private Long auctionId;
    private Long productId;
    private Long userId;
    private Long startingBid;
    private Long bidStep;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private LocalDateTime createdAt;

    // 정적 팩토리 메서드 (from)
    public static AuctionCreateResponse from(Auction auction) {
        return AuctionCreateResponse.builder()
                .auctionId(auction.getId())
                .productId(auction.getProduct().getId())
                .userId(auction.getProduct().getUser().getId())
                .startingBid(auction.getStartingBid())
                .bidStep(auction.getBidStep())
                .startTime(auction.getStartTime())
                .endTime(auction.getEndTime())
                .status(auction.getStatus())
                .createdAt(auction.getCreatedAt())
                .build();
    }
}
