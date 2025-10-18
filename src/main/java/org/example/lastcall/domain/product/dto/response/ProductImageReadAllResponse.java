package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.springframework.data.domain.Page;

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

    public static PageResponse<ProductImageReadAllResponse> from(Page<ProductImage> productImages) {
        List<ProductImageReadAllResponse> mapped = productImages.stream()
                .map(productImage -> new ProductImageReadAllResponse(
                        productImage.getProduct().getId(),
                        productImage.getImageType(),
                        productImage.getImageUrl()
                )).toList();
        return PageResponse.of(productImages, mapped);
    }
}
