package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService implements ProductServiceApi {
    private final ProductRepository productRepository;

    public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
        User user = UserRepository.findById(userId)
                .orElseThorw(() -> new BusinessException(ProductErrorCode.USER_NOT_FOUND));
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }
}
