package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;

import java.util.List;

@Getter
public class ProductImageReadAllResponse {
    private final Long productId;
    private final ImageType imageType;
    private final String imageUrl;

    public ProductImageReadAllResponse(Long productId, ImageType imageType, String imageUrl) {
        this.productId = productId;
        this.imageType = imageType;
        this.imageUrl = imageUrl;
    }

    public static List<ProductImageReadAllResponse> from(List<ProductImage> productImages) {
        return productImages.stream()
                .map(productImage -> new ProductImageReadAllResponse(
                        productImage.getId(),
                        productImage.getImageType(),
                        productImage.getImageUrl()
                )).toList();
    }
}
