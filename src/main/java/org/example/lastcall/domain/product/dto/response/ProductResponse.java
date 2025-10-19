package org.example.lastcall.domain.product.dto.response;

import lombok.Getter;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;

import java.time.LocalDateTime;

@Getter
public class ProductResponse {
    private final Long id;
    private final Long userId;
    private final String name;
    private final Category category;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime modifiedAt;

    public ProductResponse(Long id, Long userId, String name, Category category, String description, LocalDateTime createdAt, LocalDateTime modifiedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getUser().getId(),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }
}
