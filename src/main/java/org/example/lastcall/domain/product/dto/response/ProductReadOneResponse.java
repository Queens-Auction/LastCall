package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ProductReadOneResponse {

    private final Long id;
    private final Long userId;
    private final String name;
    private final Category category;
    private final String description;
    private final List<ProductImageResponse> images;
    private final LocalDateTime createdAt;
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

