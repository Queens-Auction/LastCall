package org.example.lastcall.fixture;

import org.example.lastcall.domain.point.dto.request.PointCreateRequest;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.springframework.test.util.ReflectionTestUtils;

public class PointFixture {
	public static PointCreateRequest pointCreateRequest() {
		PointCreateRequest request = new PointCreateRequest();
		ReflectionTestUtils.setField(request, "userId", 1L);
		ReflectionTestUtils.setField(request, "bidId", 10L);
		ReflectionTestUtils.setField(request, "type", PointLogType.EARN);
		ReflectionTestUtils.setField(request, "description", "포인트 등록");
		ReflectionTestUtils.setField(request, "incomePoint", 100L);

		return request;
	}

	public static PointCreateRequest pointCreateRequest(Long incomPoint) {
		PointCreateRequest request = new PointCreateRequest();
		ReflectionTestUtils.setField(request, "userId", 1L);
		ReflectionTestUtils.setField(request, "bidId", 10L);
		ReflectionTestUtils.setField(request, "type", PointLogType.EARN);
		ReflectionTestUtils.setField(request, "description", "포인트 등록");
		ReflectionTestUtils.setField(request, "incomePoint", incomPoint);

		return request;
	}
}
