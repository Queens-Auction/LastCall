package org.example.lastcall.domain.product.sevice;

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

    // 상품 이미지 등록 시 중복 썸네일 체크 포함
    public void validateThumbnailConsistency(Long productId, List<ProductImage> newImages) {
        // 1. DB에 이미 존재하는 해당 상품 썸네일 조회
        Optional<ProductImage> existingThumbnails = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);

        // 2. 요청에서 썸네일로 지정된 이미지 개수 카운트
        long newThumbnailCount = newImages.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .count();

        long totalThumbnails = (existingThumbnails.isPresent() ? 1 : 0) + newThumbnailCount;

        // 3. DB + 요청 합쳐서 1개 이상이면 예외
        if (totalThumbnails > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        // 4. 새 요청에 썸네일이 없으면 첫 번째 이미지로 자동 지정
        if (newThumbnailCount == 0 && !newImages.isEmpty()) {
            newImages.get(0).markAsThumbnail();
        }
    }

}
