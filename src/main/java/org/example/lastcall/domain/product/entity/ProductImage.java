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

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    @Column(length = 500, nullable = false)
    private String imageUrl;

    @Builder(access = AccessLevel.PRIVATE)
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
