package org.example.lastcall.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.enums.Category;

import java.time.LocalDateTime;

@Schema(description = "상품 등록/수정 응답 DTO")
@Getter
public class ProductResponse {
    @Schema(description = "상품 ID", example = "101")
    private final Long id;

    @Schema(description = "등록한 사용자 ID", example = "5")
    private final Long userId;

    @Schema(description = "상품명", example = "빈티지 롤렉스 서브마리너")
    private final String name;

    @Schema(description = "상품 카테고리", example = "WATCH")
    private final Category category;

    @Schema(description = "상품 설명", example = "1960년대 한정판 롤렉스 서브마리너 모델로, 상태가 매우 양호합니다.")
    private final String description;

    @Schema(description = "상품 등록일", example = "2025-10-24T12:34:56")
    private final LocalDateTime createdAt;

    @Schema(description = "상품 수정일", example = "2025-10-24T14:00:00")
    private final LocalDateTime modifiedAt;

    private ProductResponse(Long id, Long userId, String name, Category category, String description, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getUser().getId(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }
}
