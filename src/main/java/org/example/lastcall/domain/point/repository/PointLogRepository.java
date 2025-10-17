package org.example.lastcall.domain.point.repository;

import org.example.lastcall.domain.point.entity.PointLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {
}
