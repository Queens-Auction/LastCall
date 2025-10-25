package org.example.lastcall.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상품 단건 조회 응답 DTO (상품 기본 정보 + 이미지 목록 포함)")
@Getter
public class ProductReadOneResponse {
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

    @Schema(description = "상품 이미지 목록", example = "[{\"imageUrl\": \"https://cdn.lastcall.com/images/product_101_main.jpg\"}]")
    private final List<ProductImageResponse> images;

    @Schema(description = "상품 등록일", example = "2025-10-24T12:34:56")
    private final LocalDateTime createdAt;

    @Schema(description = "상품 수정일", example = "2025-10-24T14:00:00")
    private final LocalDateTime modifiedAt;

    private ProductReadOneResponse(Long id, Long userId, String name, Category category, String description,
                                   List<ProductImageResponse> images, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.images = images;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static ProductReadOneResponse from(Product product, List<ProductImageResponse> images) {
        return new ProductReadOneResponse(
                product.getId(),
                product.getUser().getId(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                images,
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }
}

