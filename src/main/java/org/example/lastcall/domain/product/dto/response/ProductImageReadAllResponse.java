package org.example.lastcall.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "상품 이미지 전체 조회 응답 DTO")
@Getter
public class ProductImageReadAllResponse {
    @Schema(description = "상품 ID", example = "101")
    private final Long productId;

    @Schema(description = "이미지 유형 (대표/일반)", example = "THUMBNAIL")
    private final ImageType imageType;

    @Schema(description = "이미지 URL", example = "https://cdn.lastcall.com/images/product_101_main.jpg")
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
