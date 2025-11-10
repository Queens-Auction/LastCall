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
@Table(
        name = "product_image",
        indexes = {
                // 인텔리제이에서 조회 속도 테스트 시 데이터가 적어 효과가 미비함
                // -> nGrinder로 부하테스트에서 성능 확인 예정
                // -> 부하 테스트에서도 효과가 미비하면 삭제 예정
                @Index(name = "idx_product_image_product_deleted", columnList = "product_id, deleted")
        })
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

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    private ProductImage(Product product, ImageType imageType, String imageKey, String fileHash) {
        this.product = product;
        this.imageType = imageType;
        this.imageKey = imageKey;
        this.fileHash = fileHash;
    }

    public static ProductImage of(Product product, ImageType imageType, String imageKey, String fileHash) {
        return new ProductImage(product, imageType, imageKey, fileHash);
    }

    public void updateImageType(ImageType imageType) {
        this.imageType = imageType;
    }
}
