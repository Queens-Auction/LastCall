package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductImageService implements ProductImageServiceApi {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    public ProductImageResponse createProductImage(Long productId, ProductImageCreateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        ProductImage productImage = ProductImage.of(product, request.getImageType(), request.getImageUrl());
        ProductImage savedProductImage = productImageRepository.save(productImage);

        return ProductImageResponse.from(savedProductImage);
    }

    //이미지 전체 조회(상품아이디와 썸네일만 "/api/v1/products/image?imageType=Thumbnail")
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductImageReadAllResponse> readAllThumbnailImage(ImageType imageType, int page, int size) {
        Page<ProductImage> productImages = productImageRepository.findAllByImageType(imageType, PageRequest.of(page, size));
        return ProductImageReadAllResponse.from(productImages);
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
        return ProductImageResponse.from(productImages);
    }
}
