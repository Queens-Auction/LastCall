package org.example.lastcall.domain.point.service;

public interface PointServiceApi {

    void validateSufficientPoints(Long userId, Long requiredAmount);

    void updateDepositPoint(Long auctionId, Long bidId, Long bidAmount, Long userId);
}
