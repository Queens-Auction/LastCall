package org.example.lastcall.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Schema(description = "상품 전체 조회 응답 DTO (상품 ID, 이름, 썸네일 정보 포함)")
@Getter
public class ProductReadAllResponse {
    @Schema(description = "상품 ID", example = "101")
    private final Long id;

    @Schema(description = "상품명", example = "빈티지 롤렉스 서브마리너")
    private final String name;

    @Schema(description = "대표 이미지(썸네일) URL", example = "https://cdn.lastcall.com/images/product_101_main.jpg")
    private final String thumbnailUrl;

    public ProductReadAllResponse(Long id, String name, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static PageResponse<ProductReadAllResponse> from(Page<Product> products, Map<Long, String> thumbnailUrls) {
        List<ProductReadAllResponse> mapped = products.stream()
                .map(product -> new ProductReadAllResponse(product.getId(),
                        product.getName(),
                        thumbnailUrls.get(product.getId())
                ))
                .toList();
        return PageResponse.of(products, mapped);
    }
}
