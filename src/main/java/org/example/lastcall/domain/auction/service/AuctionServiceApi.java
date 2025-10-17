package org.example.lastcall.domain.auction.service;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;

public interface AuctionServiceApi {
    AuctionResponse createAuction(Long userId, Long productId, AuctionCreateRequest request);
}
