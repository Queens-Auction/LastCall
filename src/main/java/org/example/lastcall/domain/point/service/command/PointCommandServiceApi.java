package org.example.lastcall.domain.point.service.command;

public interface PointCommandServiceApi {
    void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId);

    void depositToSettlement(Long auctionId);

    void depositToAvailablePoint(Long auctionId);
}
