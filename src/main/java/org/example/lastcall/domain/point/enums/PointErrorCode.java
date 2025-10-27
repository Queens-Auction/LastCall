package org.example.lastcall.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "가용 포인트가 부족합니다."),
    INSUFFICIENT_DEPOSIT_POINT(HttpStatus.BAD_REQUEST, "예치 포인트가 부족합니다"),
    POINT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 포인트 계좌가 존재하지 않습니다."),
    POINT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 포인트 기록을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다.");


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
