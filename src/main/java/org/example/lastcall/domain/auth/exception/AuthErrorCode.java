package org.example.lastcall.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다.");

    private final HttpStatus status;
    private final String message;
}
