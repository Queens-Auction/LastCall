package org.example.lastcall.domain.auth.email.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements ErrorCode {
    MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "이메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    NOT_REQUESTED(HttpStatus.BAD_REQUEST, "인증 요청 이력이 없습니다."),
    NOT_VERIFIED(HttpStatus.BAD_REQUEST, "인증되지 않은 이메일입니다."),
    EXPIRED(HttpStatus.BAD_REQUEST, "이메일 인증 시간이 만료되었습니다."),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
    COOLDOWN_NOT_PASSED(HttpStatus.TOO_MANY_REQUESTS, "재요청까지 대기 시간이 남아 있습니다.");

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
