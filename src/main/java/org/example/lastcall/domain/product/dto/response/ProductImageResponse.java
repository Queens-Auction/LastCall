package org.example.lastcall.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "상품 이미지 응답 DTO")
@Getter
public class ProductImageResponse {
    @Schema(description = "이미지 ID", example = "12")
    private final Long id;

    @Schema(description = "상품 ID", example = "101")
    private final Long productId;

    @Schema(description = "이미지 유형 (대표/일반)", example = "THUMBNAIL")
    private final ImageType imageType;

    @Schema(description = "이미지 URL", example = "https://cdn.lastcall.com/images/product_101_main.jpg")
    private final String imageUrl;

    @Schema(description = "이미지 등록일", example = "2025-10-24T12:34:56")
    private final LocalDateTime createdAt;

    @Schema(description = "이미지 수정일", example = "2025-10-24T13:00:00")
    private final LocalDateTime modifiedAt;

    public ProductImageResponse(Long id, Long productId, ImageType imageType, String imageUrl, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.productId = productId;
        this.imageType = imageType;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static ProductImageResponse from(ProductImage productImage) {
        return new ProductImageResponse(
                productImage.getId(),
                productImage.getProduct().getId(),
                productImage.getImageType(),
                productImage.getImageUrl(),
                productImage.getCreatedAt(),
                productImage.getModifiedAt());
    }

    public static List<ProductImageResponse> from(List<ProductImage> productImages) {
        return productImages.stream()
                .map(productImage -> new ProductImageResponse(
                        productImage.getId(),
                        productImage.getProduct().getId(),
                        productImage.getImageType(),
                        productImage.getImageUrl(),
                        productImage.getCreatedAt(),
                        productImage.getModifiedAt()
                )).toList();
    }
}