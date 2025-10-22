package org.example.lastcall.domain.auction.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

@Getter
public class MyParticipatedResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private String productDescription;
    private Long currentBid;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long myBidAmount;  // 단건 -> 내 최고 입찰가
    private Boolean isWinning; // 단건 -> 나의 낙찰 여부
}
