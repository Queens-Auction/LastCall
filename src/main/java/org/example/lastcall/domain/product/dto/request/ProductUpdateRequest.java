package org.example.lastcall.domain.product.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;

@Getter
@AllArgsConstructor
public class ProductUpdateRequest {
    @Size(min = 1, max = 80)
    private String name;

    private Category category;

    @Size(min = 30, max = 500, message = "상품 설명은 최소 30자 이상 입력해야 합니다.")
    private String description;
}
