package org.example.lastcall.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.product.enums.Category;
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

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    private Product(User user, String name, Category category, String description) {
        this.user = user;
        this.name = name;
        this.category = category;
        this.description = description;
    }

    public static Product of(User user, String name, Category category, String description) {
        return new Product(user, name, category, description);
    }

    public void updateProducts(String name, Category category, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }
}
