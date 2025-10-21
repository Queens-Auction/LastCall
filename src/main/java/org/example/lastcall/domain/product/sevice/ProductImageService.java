package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService implements ProductImageServiceApi {
    private final ProductServiceApi productServiceApi;
    private final ProductImageRepository productImageRepository;

    //이미지 등록 (여러 장 등록)
    public List<ProductImageResponse> createProductImages(Long productId, List<ProductImageCreateRequest> requests) {
        Product product = productServiceApi.findById(productId);

        validateImageCount(requests);
        validateDuplicateUrls(requests, productId);

        //프론트에서 선택한 이미지(isThumbnail()=true)가 대표 이미지가 되도록 설정
        List<ProductImage> images = requests.stream()
                .map(req -> {
                    ImageType type = (req.getIsThumbnail() != null && req.getIsThumbnail())
                            ? ImageType.THUMBNAIL
                            : ImageType.DETAIL;
                    return ProductImage.of(product, type, req.getImageUrl());
                })
                .toList();

        ensureSingleThumbnail(images);

        List<ProductImage> savedImages = productImageRepository.saveAll(images);

        return savedImages.stream()
                .map(ProductImageResponse::from)
                .toList();
    }

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

    //썸네일 이미지 변경
    public List<ProductImageResponse> updateThumbnailImage(Long productId, Long newThumbnailImageId) {
        //기존 썸네일 찾기
        Optional<ProductImage> currentThumbnail = productImageRepository.findByProductIdAndImageType(productId, ImageType.THUMBNAIL);

        //기존 썸네일이 있으면 일반 이미지로 변경하기
        currentThumbnail.ifPresent(image -> image.updateImageType(ImageType.DETAIL));

        //새로 선택한 이미지를 썸네일로 변경
        ProductImage newThumbnail = productImageRepository.findById(newThumbnailImageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        newThumbnail.updateImageType(ImageType.THUMBNAIL);

        // 변경 이후의 해당 상품의 전체 이미지 목록 반환
        List<ProductImage> productImages = productImageRepository.findAllByProductId(productId);

        // 썸네일 갯수 검사
        long thumbnailCount = productImageRepository.countByProductIdAndImageType(productId, ImageType.THUMBNAIL);
        if (thumbnailCount > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        return ProductImageResponse.from(productImages);
    }

    //이미지 갯수 제한 매서드
    private void validateImageCount(List<ProductImageCreateRequest> requests) {
        if (requests.size() > 10) {
            throw new BusinessException(ProductErrorCode.TOO_MANY_IMAGES);
        }
    }

    //이미지 중복 검사 메서드
    private void validateDuplicateUrls(List<ProductImageCreateRequest> requests, Long productId) {
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

    //대표 썸네일 지정 메서드
    private void ensureSingleThumbnail(List<ProductImage> images) {
        long thumbnailCount = images.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .count();
        if (thumbnailCount > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }
        //썸네일 없을 경우 첫 번째 이미지로 결정
        if (thumbnailCount == 0 && !images.isEmpty()) {
            images.get(0).markAsThumbnail();
        }
    }
}
