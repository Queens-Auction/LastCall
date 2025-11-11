package org.example.lastcall.domain.point.repository;

import java.util.List;

import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
	boolean existsByAuctionIdAndTypeAndUserId(Long id, PointLogType pointLogType, Long loserId);

	// select 1 from point_log where bid_id = {bidId} and type in (types.....)
	boolean existsByBidIdAndTypeIn(Long bidId, List<PointLogType> deposit);
}
