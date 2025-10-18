package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ProductImageResponse {
    private final Long id;
    private final Long productId;
    private final ImageType imageType;
    private final String imageUrl;
    private final LocalDateTime createdAt;
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