package org.example.lastcall.domain.product.repository;

import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findAllByProductIdAndDeletedFalse(Long productId);

    Optional<ProductImage> findByProductIdAndImageTypeAndDeletedFalse(Long productId, ImageType imageType);

    long countByProductIdAndImageType(Long productId, ImageType imageType);

    @Modifying
    @Query("UPDATE ProductImage i SET i.deleted = true WHERE i.product.id = :productId")
    void softDeleteByProductId(Long productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds AND pi.imageType = 'THUMBNAIL' AND pi.deleted = false")
    List<ProductImage> findAllThumbnailsByProductIds(@Param("productIds") List<Long> productIds);

    List<ProductImage> findByProductIdAndDeletedFalseOrderByIdAsc(Long productId);

    Optional<ProductImage> findByIdAndDeletedFalse(Long newThumbnailImageId);
}
