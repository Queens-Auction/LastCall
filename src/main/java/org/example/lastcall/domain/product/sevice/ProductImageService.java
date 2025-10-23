package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService implements ProductImageServiceApi {
    // private final ProductCommandServiceApi productServiceApi; -> 순환 참조 문제 발생
    private final ProductImageRepository productImageRepository;
    private final AuctionServiceApi auctionServiceApi;

    //이미지 등록 (여러 장 등록)
    @Override
    public List<ProductImageResponse> createProductImages(Product product, List<ProductImageCreateRequest> requests) {
        validateImageCount(requests);
        validateDuplicateUrls(requests, product.getId());

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

    //상품 수정 시 이미지 추가
    @Override
    public List<ProductImageResponse> appendProductImages(Product product, List<ProductImageCreateRequest> requests) {
        auctionServiceApi.validateAuctionScheduled(product.getId());

        //기존 이미지 불러오기
        List<ProductImage> existingImages = productImageRepository.findAllByProductId(product.getId());

        //새 이미지 객체생성
        List<ProductImage> newImages = requests.stream()
                .map(req -> {
                    ImageType type = (req.getIsThumbnail() != null && req.getIsThumbnail())
                            ? ImageType.THUMBNAIL
                            : ImageType.DETAIL;
                    return ProductImage.of(product, type, req.getImageUrl());
                })
                .toList();

        //전체 이미지 합치기
        List<ProductImage> allImages = new ArrayList<>();
        allImages.addAll(existingImages);
        allImages.addAll(newImages);

        //총 이미지 갯수 검증
        if (allImages.size() > 10) {
            throw new BusinessException(ProductErrorCode.MAX_IMAGE_COUNT_EXCEEDED);
        }

        //썸네일 한 개만 유지
        ensureSingleThumbnail(allImages);

        //중복 URL 체크
        validateDuplicateUrlsForAll(allImages);

        //새 이미지들만 저장
        List<ProductImage> savedImages = productImageRepository.saveAll(newImages);

        return savedImages.stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    private void validateDuplicateUrlsForAll(List<ProductImage> allImages) {
        Set<String> urls = new HashSet<>();
        for (ProductImage image : allImages) {
            if (!urls.add(image.getImageUrl())) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
            }
        }
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

    //이미지 삭제
    public void deleteProductImage(Long productId, Long imageId) {
        auctionServiceApi.validateAuctionScheduled(productId);
        ProductImage productImage = productImageRepository.findById(imageId).orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new BusinessException(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT);
        }
        productImage.softDelete();
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

    @Override
    public void softDeleteByProductId(Long productId) {
        productImageRepository.softDeleteByProductId(productId);
    }
}
