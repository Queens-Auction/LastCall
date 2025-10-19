package org.example.lastcall.domain.auction.dto.response;

import lombok.Getter;

@Getter
public class AuctionReadAllResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private Integer participantCount;
}
