package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductResponse;

public interface ProductServiceApi {
    ProductResponse createProduct(Long userId, ProductCreateRequest request);
}