package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.entity.Product;

public interface ProductViewServiceApi {
    //상품 조회 : 없으면 예외처리
    Product findById(Long productId);

}
