package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;

public interface ProductImageServiceApi {
    ProductImageResponse createProductImage(Long productId, ProductImageCreateRequest request);
}
