package org.example.lastcall.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    // 인증/인가 공통
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 비밀번호 검증 실패 (세분화 유지 시)
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // 계정 상태
    ACCOUNT_DELETED(HttpStatus.GONE, "탈퇴한 계정입니다."),

    // 리프레시 토큰/세션 계열
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    REVOKED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "폐기된 리프레시 토큰입니다.");

    private final HttpStatus status;
    private final String message;
}
