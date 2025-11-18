package org.example.lastcall.domain.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 이미지 타입 Enum")
public enum ImageType {
    @Schema(description = "대표 이미지 (썸네일)")
    THUMBNAIL,

    @Schema(description = "상세 이미지 (상품 설명용)")
    DETAIL
}
