package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
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
}
