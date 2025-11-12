package org.example.lastcall.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_EMPTY_EMAIL_OR_PASSWORD(HttpStatus.BAD_REQUEST, "이메일과 비밀번호는 공백일 수 없습니다."),

    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    ACCOUNT_DELETED(HttpStatus.GONE, "탈퇴한 계정입니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다."),
    REVOKED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "폐기된 리프레시 토큰입니다."),

    MISSING_PASSWORD(HttpStatus.BAD_REQUEST, "회원 탈퇴를 위해 비밀번호를 입력해야 합니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인후 진행해주세요");


    private final HttpStatus status;
    private final String message;
}
