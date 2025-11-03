package org.example.lastcall.domain.point.service.command;

public interface PointCommandServiceApi {
	void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId);

	void depositToSettlement(Long userId, Long auctionId, Long amount);

	void depositToAvailablePoint(Long userId, Long auctionId, Long amount);
}
