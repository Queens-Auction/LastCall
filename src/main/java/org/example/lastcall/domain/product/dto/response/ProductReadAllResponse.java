package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.product.entity.Product;

import java.util.List;

@Getter
public class ProductReadAllResponse {
    private final Long id;
    private final String name;

    public ProductReadAllResponse(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static List<ProductReadAllResponse> from(List<Product> products) {
        return products.stream()
                .map(product -> new ProductReadAllResponse(
                        product.getId(),
                        product.getName()
                ))
                .toList();
    }
}
