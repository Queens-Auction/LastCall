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
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductValidator {
    private final ProductImageRepository productImageRepository;

    //이미지 중복 검사 메서드
    public void validateDuplicateFilesBeforeUpload(List<ProductImageCreateRequest> requests,
                                                   List<MultipartFile> images,
                                                   Long productId) {
        if (requests.size() != images.size()) {
            throw new BusinessException(ProductErrorCode.INVALID_REQUEST);
        }
        //요청 내부 중복 검사
        Set<String> fileNameSet = new HashSet<>();
        for (MultipartFile file : images) {
            String fileName = file.getOriginalFilename();
            if (!fileNameSet.add(fileName)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_REQUEST);
            }
        }

        // 같은 상품 내 DB 이미지 중복 검사
        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);
        Set<String> existingFileNames = existingImages.stream()
                .map(img -> Paths.get(img.getImageUrl()).getFileName().toString())
                .collect(Collectors.toSet());
        for (String fileName : fileNameSet) {
            if (existingFileNames.contains(fileName)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
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
