package org.example.lastcall.domain.auction.service.query;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionDetailService implements AuctionFinder {
    private final AuctionRepository auctionRepository;

    // 경매 ID로 경매 조회
    @Override
    public Auction findById(Long auctionId) {
        return auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND)
        );
    }
}
