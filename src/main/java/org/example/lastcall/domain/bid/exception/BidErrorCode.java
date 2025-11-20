package org.example.lastcall.domain.bid.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BidErrorCode implements ErrorCode {
    SELLER_CANNOT_BID(HttpStatus.FORBIDDEN, "판매자는 자신의 경매에 입찰할 수 없습니다."),

    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 비드가 존재하지 않습니다."),
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "입찰 금액이 현재 최고가 이하입니다."),
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "요청한 입찰 금액이 유효하지 않습니다."),

    CONCURRENCY_BID_FAILED(HttpStatus.CONFLICT, "이미 다른 사용자가 먼저 입찰했습니다. 다시 시도해주세요."),
    FIRST_BID_TOO_LOW(HttpStatus.BAD_REQUEST, "첫 입찰 금액이 시작가보다 낮습니다.");

    private final HttpStatus status;
    private final String message;
}
