package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductViewService implements ProductViewServiceApi {
    private final ProductRepository productRepository;
    private final ProductImageViewServiceApi productImageViewServiceApi;

    //상품 전체 조회(상품 아이디와 상품명만 조회 : 내 상품 관리용 상품 전체 조회)
    public PageResponse<ProductReadAllResponse> readAllProduct(int page, int size) {
        Page<Product> products = productRepository.findAll(PageRequest.of(page, size));
        return ProductReadAllResponse.from(products);
    }

    //상품 단건 조회
    public ProductReadOneResponse readProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        List<ProductImageResponse> images = productImageViewServiceApi.readAllProductImage(productId);

        return ProductReadOneResponse.from(product, images);
    }

    //상품 찾기
    @Override
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
