package org.example.lastcall.domain.auction.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuctionErrorCode implements ErrorCode {
    UNAUTHORIZED_SELLER(HttpStatus.FORBIDDEN, "해당 상품의 소유자가 아닙니다."),
    DUPLICATE_AUCTION(HttpStatus.CONFLICT, "해당 상품의 경매가 이미 존재합니다."),

    CANNOT_MODIFY_PRODUCT_DURING_AUCTION(HttpStatus.FORBIDDEN, "시작되지 않은 경매만 상품을 수정하거나 삭제할 수 있습니다."),
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 경매가 존재하지 않습니다."),
    CANNOT_BID_ON_NON_ONGOING_AUCTION(HttpStatus.FORBIDDEN, "진행 중인 경매일 경우에만 입찰이 가능합니다."),
    CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION(HttpStatus.FORBIDDEN, "진행 중이거나 이미 종료된 경매는 수정할 수 없습니다."),
    USER_NOT_PARTICIPATED_IN_AUCTION(HttpStatus.NOT_FOUND, "해당 경매에 참여한 사용자가 아닙니다."),
    AUCTION_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다."),

    INVALID_START_TIME(HttpStatus.BAD_REQUEST, "시작일은 현재 시각 이후여야 합니다."),
    INVALID_END_TIME(HttpStatus.BAD_REQUEST, "종료일은 현재 시각 이후여야 합니다."),
    INVALID_END_TIME_ORDER(HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다."),
    INVALID_SAME_TIME(HttpStatus.BAD_REQUEST, "시작일과 종료일이 같을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
