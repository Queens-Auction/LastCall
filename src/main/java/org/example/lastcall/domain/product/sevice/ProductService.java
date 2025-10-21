package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService implements ProductServiceApi {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.USER_NOT_FOUND));
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    //상품 전체 조회(상품 아이디와 상품명만 조회 : 내 상품 관리용 상품 전체 조회)
    @Transactional(readOnly = true)
    public PageResponse<ProductReadAllResponse> readAllProduct(int page, int size) {
        Page<Product> products = productRepository.findAll(PageRequest.of(page, size));
        return ProductReadAllResponse.from(products);
    }

    //상품 단건 조회
    @Override
    @Transactional(readOnly = true)
    public ProductResponse readProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return ProductResponse.from(product);
    }

    //상품 정보 수정
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());
        return ProductResponse.from(product);
    }

    @Override
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
}
