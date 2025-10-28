package org.example.lastcall.domain.point.service.query;

public interface PointQueryServiceApi {
	void validateSufficientPoints(Long userId, Long requiredAmount);
}
