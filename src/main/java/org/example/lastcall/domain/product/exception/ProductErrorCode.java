package org.example.lastcall.domain.product.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품이 존재하지 않습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "사진이 존재하지 않습니다."),
    TOO_MANY_IMAGES(HttpStatus.BAD_REQUEST, "사진은 최대 10장 첨부할 수 있습니다."),
    MULTIPLE_THUMBNAILS_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "대표 이미지는 1장만 등록 가능합니다."),
    DUPLICATE_IMAGE_URL_IN_REQUEST(HttpStatus.BAD_REQUEST, "중복 이미지입니다."),
    DUPLICATE_IMAGE_URL_IN_PRODUCT(HttpStatus.BAD_REQUEST, "한 상품 안에 중복 이미지는 들어갈 수 없습니다."),
    THUMBNAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "대표 이미지가 존재하지 않습니다."),
    IMAGE_NOT_BELONGS_TO_PRODUCT(HttpStatus.BAD_REQUEST, "해당 상품에 속한 이미지가 아닙니다."),
    MAX_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 10장입니다."),
    UNAUTHORIZED_PRODUCT_OWNER(HttpStatus.FORBIDDEN, "상품의 소유자가 아닙니다.");

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
