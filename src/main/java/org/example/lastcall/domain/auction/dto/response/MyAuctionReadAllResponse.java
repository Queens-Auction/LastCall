package org.example.lastcall.domain.auction.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyAuctionReadAllResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private String productDescription;
    private int currentBid;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
