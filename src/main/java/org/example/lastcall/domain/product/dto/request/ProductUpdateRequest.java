package org.example.lastcall.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;

@Schema(description = "상품 수정 요청 DTO")
@Getter
@AllArgsConstructor
public class ProductUpdateRequest {
    @Schema(description = "상품명", example = "에어팟 프로 2세대")
    @Size(min = 1, max = 80)
    private String name;

    @Schema(description = "상품 카테고리", example = "ELECTRONICS")
    private Category category;

    @Schema(description = "상품 설명", example = "최신형 에어팟 프로 2세대 제품으로, 노이즈 캔슬링과 투명 모드를 지원합니다.")
    @Size(min = 30, max = 500, message = "상품 설명은 최소 30자 이상 입력해야 합니다.")
    private String description;
}
