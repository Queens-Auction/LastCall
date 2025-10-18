package org.example.lastcall.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;

@Getter
public class ProductCreateRequest {
    @NotBlank(message = "상품명은 필수 입력값입니다.")
    @Size(min = 1, max = 80)
    private String name;

    @NotNull(message = "상품 카테고리는 필수 입력값입니다.")
    private Category category;

    @NotBlank(message = "상품 설명은 필수 입력값입니다.")
    @Size(min = 30, max = 500, message = "상품 설명은 최소 30자 이상 입력해야 합니다.")
    private String description;
}
