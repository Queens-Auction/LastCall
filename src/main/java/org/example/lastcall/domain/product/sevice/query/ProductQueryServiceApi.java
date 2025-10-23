package org.example.lastcall.domain.product.sevice.query;

import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;

import java.util.List;

public interface ProductQueryServiceApi {
    //상품 조회 : 없으면 예외처리
    Product findById(Long productId);

    ProductImageResponse readThumbnailImage(Long productId);

    List<ProductImageResponse> readAllProductImage(Long productId);
}
