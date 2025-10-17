package org.example.lastcall.domain.auction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuctionErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품이 존재하지 않습니다.");

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
