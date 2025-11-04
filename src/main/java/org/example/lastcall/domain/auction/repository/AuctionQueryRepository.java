package org.example.lastcall.domain.auction.repository;

import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.product.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface AuctionQueryRepository {
    // 경매 전체 조회
    Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable);

    // 내가 참여한 경매 목록 조회
    Page<MyParticipatedResponse> findMyParticipatedAuctions(Long userId, Pageable pageable);

    // 내가 참여한 경매 단건 조회
    Optional<MyParticipatedResponse> findMyParticipatedAuctionDetail(Long auctionId, Long userId);
}
