package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductImageViewService implements ProductImageViewServiceApi {
    private final ProductImageRepository productImageRepository;

    //대표이미지 조회
    @Override
    @Transactional(readOnly = true)
    public ProductImageResponse readThumbnailImage(Long productId) {
        ProductImage thumbnailImage = productImageRepository.findByProductIdAndImageType(productId, ImageType.THUMBNAIL)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.THUMBNAIL_NOT_FOUND));
        return ProductImageResponse.from(thumbnailImage);
    }

    //상품별 이미지 전체 조회
    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> readAllProductImage(Long productId) {
        List<ProductImage> productImages = productImageRepository.findAllByProductId(productId);
        return ProductImageResponse.from(productImages);
    }
}
