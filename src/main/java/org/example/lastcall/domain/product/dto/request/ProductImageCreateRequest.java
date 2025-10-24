package org.example.lastcall.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "상품 이미지 등록 요청 DTO")
@Getter
@AllArgsConstructor
public class ProductImageCreateRequest {
    @Schema(description = "대표 이미지 여부", example = "true")
    @NotNull
    private Boolean isThumbnail;

    @Schema(description = "이미지 URL", example = "https://cdn.lastcall.com/images/product_123_main.jpg")
    @NotBlank
    private String imageUrl;
}
