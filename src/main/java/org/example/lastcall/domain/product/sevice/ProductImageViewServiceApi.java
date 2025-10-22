package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageViewServiceApi {
    // 대표 이미지(썸네일) 조회
    ProductImageResponse readThumbnailImage(Long productId);

    // 상품별 이미지리스트 조회
    List<ProductImageResponse> readAllProductImage(Long productId);
}
