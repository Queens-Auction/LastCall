package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageServiceApi {
    ProductImageResponse createProductImage(Long productId, ProductImageCreateRequest request);

    //전체 상품 대표 이미지 조회
    PageResponse<ProductImageReadAllResponse> readAllThumbnailImage(int page, int size);

    //상품 이미지 전체 조회
    List<ProductImageResponse> readAllProductImage(Long productId);
}
