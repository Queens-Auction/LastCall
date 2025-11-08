package org.example.lastcall.domain.product.service.validator;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductValidator {
    private final ProductImageRepository productImageRepository;

    public void checkOwnership(Product product, AuthUser authUser) {
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }
    }

    //이미지 갯수 제한 매서드
    public <T> void validateImageCount(List<T> images) {
        if (images.size() > 10) {
            throw new BusinessException(ProductErrorCode.TOO_MANY_IMAGES);
        }
    }

    public void validateThumbnailConsistencyForCreate(Long productId, List<ProductImageCreateRequest> requests) {
        boolean hasExistingThumbnail = productImageRepository
                .existsByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);
        long newThumbnailCount = requests.stream()
                .filter(req -> req.getIsThumbnail() == true)
                .count();
        long totalThumbnails = (hasExistingThumbnail ? 1 : 0) + newThumbnailCount;
        if (totalThumbnails > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        if (newThumbnailCount == 0 && !requests.isEmpty() && !hasExistingThumbnail) {
            requests.set(0, requests.get(0).toBuilder().isThumbnail(true).build());
        }
    }

    public void validateThumbnailConsistencyForAppend(List<ProductImage> allImages) {
        long thumbnailCount = allImages.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .count();

        if (thumbnailCount > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }
    }
}
