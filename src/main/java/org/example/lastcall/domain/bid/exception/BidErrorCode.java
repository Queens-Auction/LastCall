package org.example.lastcall.domain.bid.exception;

import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BidErrorCode implements ErrorCode {
	SELLER_CANNOT_BID(HttpStatus.FORBIDDEN, "판매자는 자신의 경매에 입찰할 수 없습니다."),    // 403 (접근 금지)
	BID_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 비드가 존재하지 않습니다.");

	private final HttpStatus status;
	private final String message;

	@Override
	public HttpStatus getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
