package org.example.lastcall.domain.auction.repository;

import java.util.Optional;

import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.product.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuctionQueryRepository {
	Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable);

	Page<MyParticipatedResponse> findMyParticipatedAuctions(Long userId, Pageable pageable);

	Optional<MyParticipatedResponse> findMyParticipatedAuctionDetail(Long auctionId, Long userId);
}
