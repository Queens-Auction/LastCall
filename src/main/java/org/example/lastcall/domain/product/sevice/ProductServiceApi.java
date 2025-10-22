package org.example.lastcall.domain.product.sevice;

import org.example.lastcall.domain.product.dto.response.ProductResponse;

// 반드시 ProductViewServiceApi로 이름 바꿀 것!
public interface ProductServiceApi {
    //상품 단건 조회
    ProductResponse readProduct(Long productId);

}
