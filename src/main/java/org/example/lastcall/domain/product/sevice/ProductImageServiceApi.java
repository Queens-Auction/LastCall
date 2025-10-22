package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;

import java.util.List;

public interface ProductImageServiceApi {
    void softDeleteByProductId(Long productId);

    List<ProductImageResponse> createProductImages(Product product, List<ProductImageCreateRequest> requests);

    // 대표 이미지(썸네일) 조회
    ProductImageResponse readThumbnailImage(Long productId);

    // 상품별 이미지리스트 조회
    List<ProductImageResponse> readAllProductImage(Long productId);
}
