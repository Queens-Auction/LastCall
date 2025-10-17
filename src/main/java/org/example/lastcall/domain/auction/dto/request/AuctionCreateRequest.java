package org.example.lastcall.domain.auction.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AuctionCreateRequest {
    @NotNull(message = "상품 id는 필수 입력값입니다.")
    private Long productId;

    @NotNull(message = "경매 시작 가격은 필수 입력값입니다.")
    private Long startingBid;

    @NotNull(message = "경매 입찰 단위는 필수 입력값입니다.")
    private Long bidStep;

    @NotNull(message = "경매 시작일은 필수 입력값입니다.")
    private LocalDateTime startingTime;

    @NotNull(message = "경매 종료일은 필수 입력값입니다.")
    private LocalDateTime endTime;
}
