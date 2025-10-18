package org.example.lastcall.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "product_image",
        //한 상품 당 하나의 이미지만 THUMBNAIL이 될 수 있도록 제약
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"product_id", "image_type"}
        ))
public class ProductImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "image_type", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Builder
    /* 클래스 위로 이동하기를 계속 시도해보았으나 모든 필드가 포함된 생성자를 private로 만들어야 함.
    클래스 레벨 @Builder를 쓰면 모든 필드가 빌더에 포함됨 → id까지 포함됨.
    하지만 id는 DB가 자동으로 생성해주므로 외부에서 넣어줄 필요가 없음 → 빌더에서 id를 빼야 함. */
    private ProductImage(Product product, ImageType imageType, String imageUrl) {
        this.product = product;
        this.imageType = imageType;
        this.imageUrl = imageUrl;
    }

    public static ProductImage of(Product product, ImageType imageType, String imageUrl) {
        return ProductImage.builder()
                .product(product)
                .imageType(imageType)
                .imageUrl(imageUrl)
                .build();
    }

    public void updateImageType(ImageType imageType) {
        this.imageType = imageType;
    }
}
