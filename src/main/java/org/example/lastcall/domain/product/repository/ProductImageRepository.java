package org.example.lastcall.domain.product.repository;

import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findAllByProductId(Long productId);

    Optional<ProductImage> findByProductIdAndImageType(Long productId, ImageType imageType);

    boolean existsByProductIdAndImageUrl(Long productId, String url);

    long countByProductIdAndImageType(Long productId, ImageType imageType);
}
