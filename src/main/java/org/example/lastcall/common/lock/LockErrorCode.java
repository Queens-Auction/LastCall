package org.example.lastcall.common.lock;

import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LockErrorCode implements ErrorCode {
	LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "현재 다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요."),
	LOCK_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

	private final HttpStatus status;
	private final String message;
}
