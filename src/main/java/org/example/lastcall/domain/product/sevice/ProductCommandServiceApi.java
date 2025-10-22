package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductCommandServiceApi {
    List<ProductImageResponse> addImagesToProduct(Long productId, List<ProductImageCreateRequest> requests);
}