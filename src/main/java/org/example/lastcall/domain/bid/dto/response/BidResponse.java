package org.example.lastcall.domain.bid.dto.response;

import java.time.LocalDateTime;

import org.example.lastcall.domain.bid.entity.Bid;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidResponse {
	private final Long id;
	private final Long auctionId;
	private final Long userId;
	private final Long bidAmount;
	private final LocalDateTime createdAt;

	public static BidResponse from(Bid bid) {
		return new BidResponse(
			bid.getId(),
			bid.getAuction().getId(),
			bid.getUser().getId(),
			bid.getBidAmount(),
			bid.getCreatedAt());
	}
}
