package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;

import java.util.List;

public interface ProductServiceApi {
    ProductResponse createProduct(Long userId, ProductCreateRequest request);

    //상품 전체 조회
    List<ProductReadAllResponse> readAllProduct();

    //상품 단건 조회
    ProductResponse readProduct(Long productId);
}