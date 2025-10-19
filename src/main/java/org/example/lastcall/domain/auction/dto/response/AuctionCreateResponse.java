package org.example.lastcall.domain.auction.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

@Getter
public class AuctionCreateResponse {
    private Long auctionId;
    private Long productId;
    private Long userId;
    private Long startingBid;
    private Long bidStep;
    private Long currentBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private Integer participantCount;
    private LocalDateTime createdAt;
}
