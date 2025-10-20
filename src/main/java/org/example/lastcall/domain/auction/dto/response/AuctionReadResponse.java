package org.example.lastcall.domain.auction.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.auction.entity.AuctionStatus;

import java.time.LocalDateTime;

@Getter
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
}
