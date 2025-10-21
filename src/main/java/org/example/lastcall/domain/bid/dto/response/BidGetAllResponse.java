package org.example.lastcall.domain.bid.dto.response;

import java.time.LocalDateTime;

import org.example.lastcall.domain.bid.entity.Bid;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidGetAllResponse {
	private final Long id;
	private final Long auctionId;
	private final String nickname;
	private final Long bidAmount;
	private final LocalDateTime createdAt;

	public static BidGetAllResponse from(Bid bid) {
		return new BidGetAllResponse(
			bid.getId(),
			bid.getAuction().getId(),
			bid.getUser().getNickname(),
			bid.getBidAmount(),
			bid.getCreatedAt());
	}
}
