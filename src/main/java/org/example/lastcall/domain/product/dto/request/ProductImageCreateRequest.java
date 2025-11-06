package org.example.lastcall.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "상품 이미지 등록 요청 DTO")
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageCreateRequest {
    @Schema(description = "대표 이미지 여부", example = "true")
    @NotNull
    private Boolean isThumbnail;
}
