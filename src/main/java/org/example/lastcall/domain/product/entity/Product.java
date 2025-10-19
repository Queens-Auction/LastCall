package org.example.lastcall.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.user.entity.User;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 80, nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(length = 500, nullable = false)
    private String description;

    @Builder
    private Product(User user, String name, Category category, String description) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public static Product of(User user, String name, Category category, String description) {
        return Product.builder()
                .user(user)
                .name(name)
                .category(category)
                .description(description)
                .build();
    }

    public void updateProducts(String name, Category category, String description) {
        this.name = name;
        this.category = category;
        this.description = description;
    }
}
