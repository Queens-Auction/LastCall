package org.example.lastcall.domain.point.repository;

import java.util.List;

import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
	boolean existsByAuctionIdAndTypeAndUserId(Long id, PointLogType pointLogType, Long loserId);

	boolean existsByBidIdAndTypeIn(Long bidId, List<PointLogType> deposit);
}
