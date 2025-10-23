package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Getter
public class ProductReadAllResponse {
    private final Long id;
    private final String name;
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
