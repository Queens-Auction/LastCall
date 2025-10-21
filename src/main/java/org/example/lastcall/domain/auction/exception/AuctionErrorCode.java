package org.example.lastcall.domain.auction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuctionErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품이 존재하지 않습니다."),
    UNAUTHORIZED_SELLER(HttpStatus.FORBIDDEN, "해당 상품의 소유자가 아닙니다."),
    DUPLICATE_AUCTION(HttpStatus.CONFLICT, "해당 상품의 경매가 이미 존재합니다."),
    CANNOT_MODIFY_PRODUCT_DURING_AUCTION(HttpStatus.FORBIDDEN, "시작되지 않은 경매만 상품을 수정하거나 삭제할 수 있습니다.");
    // AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매가 존재하지 않습니다.");

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
