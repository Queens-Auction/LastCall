package org.example.lastcall.domain.product.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.product.enums.ImageType;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product_image")
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

    private ProductImage(Product product, ImageType imageType, String imageUrl) {
        this.product = product;
        this.imageType = imageType;
        this.imageUrl = imageUrl;
    }

    public static ProductImage of(Product product, ImageType imageType, String imageUrl) {
        return new ProductImage(product, imageType, imageUrl);
    }

    public void updateImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public void markAsThumbnail() {
        this.imageType = ImageType.THUMBNAIL;
    }
}
