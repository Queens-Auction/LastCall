package org.example.lastcall.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.example.lastcall.domain.product.enums.Category;

@Schema(description = "상품 등록 요청 DTO")
@Getter
public class ProductCreateRequest {
    @Schema(description = "상품명", example = "에어팟 프로 2세대")
    @NotBlank(message = "상품명은 필수 입력값입니다.")
    @Size(min = 1, max = 80)
    private String name;

    @Schema(description = "상품 카테고리", example = "ELECTRONICS")
    @NotNull(message = "상품 카테고리는 필수 입력값입니다.")
    private Category category;

    @Schema(description = "상품 설명", example = "최신형 에어팟 프로 2세대 제품으로, 노이즈 캔슬링과 투명 모드를 지원합니다.")
    @NotBlank(message = "상품 설명은 필수 입력값입니다.")
    @Size(min = 30, max = 500, message = "상품 설명은 최소 30자 이상 입력해야 합니다.")
    private String description;
}
