package org.example.lastcall.domain.product.service.query;

import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;

import java.util.List;

public interface ProductQueryServiceApi {
    Product findById(Long productId);

    ProductImageResponse findThumbnailImage(Long productId);

    List<ProductImageResponse> findAllProductImage(Long productId);

    Product validateProductOwner(Long productId, Long userId);
}
