package org.example.lastcall.domain.point.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "Insufficient available points."),
    POINT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "User does not have a point account yet."),
    POINT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "Point record not found for this user."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User does not exist.");


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
