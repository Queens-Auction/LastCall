package org.example.lastcall.domain.auction.service.command;

import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;

public interface AuctionCommandServiceApi {

    AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request);
}
