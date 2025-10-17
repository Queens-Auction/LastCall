package org.example.lastcall.domain.point.repository;

import org.example.lastcall.domain.point.entity.Point;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<Point, Long> {
}
