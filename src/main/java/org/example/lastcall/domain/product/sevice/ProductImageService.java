package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}
