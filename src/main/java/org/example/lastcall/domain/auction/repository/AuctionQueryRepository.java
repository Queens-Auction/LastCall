package org.example.lastcall.domain.auction.repository;

import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.product.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionQueryRepository {
    // 경매 전체 조회
    Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable);
}
