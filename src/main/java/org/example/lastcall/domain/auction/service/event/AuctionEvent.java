package org.example.lastcall.domain.auction.service.event;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AuctionEvent implements Serializable {
	private Long auctionId;
	private Long winnerId;
	private Long winningBid;
	private List<Long> failedBidderIds;
	private Long version;
}
