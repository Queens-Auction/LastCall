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

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Builder
    /* 클래스 위로 이동하기를 계속 시도해보았으나 모든 필드가 포함된 생성자를 private로 만들어야 함.
    클래스 레벨 @Builder를 쓰면 모든 필드가 빌더에 포함됨 → id까지 포함됨.
    하지만 id는 DB가 자동으로 생성해주므로 외부에서 넣어줄 필요가 없음 → 빌더에서 id를 빼야 함. */
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
