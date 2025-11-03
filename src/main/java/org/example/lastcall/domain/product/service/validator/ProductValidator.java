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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductValidator {
    private final ProductImageRepository productImageRepository;

    //이미지 중복 검사 메서드 : 업로드하려는 이미지들의 중복 여부(추가 올리는 이미지도 포함)를 검사한다.
    //DB에 이미 존재하는 이미지 URL과의 중복
    //새로 업로드한 이미지들끼리의 중복
    public void validateDuplicateUrls(List<ProductImageCreateRequest> requests, Long productId) {
        // DB에 이미 저장된 이미지 URL 조회
        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);
        Set<String> existingUrls = existingImages.stream()
                .map(ProductImage::getImageUrl)
                .collect(Collectors.toSet());

        // 새로 들어온 이미지들의 URL 추출
        Set<String> newUrls = new HashSet<>();

        for (ProductImageCreateRequest req : requests) {
            // S3 업로드 이전이라면 URL 대신 파일 이름으로 비교하거나
            // 업로드 후라면 imageURL로 비교
            String fileName = req.getMultipartFile().getOriginalFilename();

            // 기존 DB에 같은 파일이 존재하면 예외 발생
            if (existingUrls.contains(fileName)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
            }

            // 요청 안에서도 중복된 파일명 있으면 예외 발생
            if (!newUrls.add(fileName)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_REQUEST);
            }
        }
    }

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
