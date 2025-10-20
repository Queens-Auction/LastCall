package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;

public interface ProductServiceApi {
    //상품 단건 조회
    ProductResponse readProduct(Long productId);

    Product findById(Long productId);
}