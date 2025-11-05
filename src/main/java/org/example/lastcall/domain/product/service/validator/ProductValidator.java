package org.example.lastcall.domain.product.service.validator;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ProductValidator {
    private final ProductImageRepository productImageRepository;

    //이미지 중복 검사 메서드
    public void validateDuplicateUrls(List<ProductImageCreateRequest> requests, Long productId) {
        //요청 내부 중복 검사
        Set<String> urlSet = new HashSet<>();
        for (ProductImageCreateRequest req : requests) {
            if (!urlSet.add(req.getImageUrl())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_REQUEST);
            }
        }

        // 같은 상품 내 DB 이미지 중복 검사
        for (String url : urlSet) {
            if (productImageRepository.existsByProductIdAndImageUrl(productId, url)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
            }
        }
    }


    public void validateDuplicateUrlsForAll(List<ProductImage> allImages) {
        Set<String> urls = new HashSet<>();
        for (ProductImage image : allImages) {
            if (!urls.add(image.getImageUrl())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
            }
        }
    }

    //이미지 갯수 제한 매서드
    public void validateImageCount(List<ProductImageCreateRequest> requests) {
        if (requests.size() > 10) {
            throw new BusinessException(ProductErrorCode.TOO_MANY_IMAGES);
        }
    }

    public void validateThumbnailConsistencyForCreate(Long productId, List<ProductImage> newImages) {
        Optional<ProductImage> existingThumbnails = productImageRepository
                .findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);

        long newThumbnailCount = newImages.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .count();

        long totalThumbnails = (existingThumbnails.isPresent() ? 1 : 0) + newThumbnailCount;

        if (totalThumbnails > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        // create일 때만 자동 지정
        if (newThumbnailCount == 0 && !newImages.isEmpty() && existingThumbnails.isEmpty()) {
            newImages.get(0).markAsThumbnail();
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
