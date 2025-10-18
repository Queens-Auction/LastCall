package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;

public interface ProductServiceApi {
    ProductResponse createProduct(Long userId, ProductCreateRequest request);

    //상품 전체 조회
    PageResponse<ProductReadAllResponse> readAllProduct(int page, int size);

    //상품 단건 조회
    ProductResponse readProduct(Long productId);
}