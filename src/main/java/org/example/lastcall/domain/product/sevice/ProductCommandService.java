package org.example.lastcall.domain.product.sevice;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService implements ProductCommandServiceApi {
    private final ProductRepository productRepository;
    private final ProductImageServiceApi productImageCommandServiceApi;
    private final AuctionServiceApi auctionServiceApi;
    private final UserServiceApi userServiceApi;

    public ProductResponse createProduct(Long userId, ProductCreateRequest request) {
        User user = userServiceApi.findById(userId);
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    //상품 정보 수정
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request) {
        auctionServiceApi.validateAuctionScheduled(productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());
        return ProductResponse.from(product);
    }

    //상품 삭제
    public void deleteProduct(Long productId) {
        //경매 중, 경매 완료인 상품은 삭제 불가능
        auctionServiceApi.validateAuctionScheduled(productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        product.softDelete();

        //상품에 연결된 이미지까지 soft delete
        productImageCommandServiceApi.softDeleteByProductId(productId);
    }

    @Override
    public List<ProductImageResponse> addImagesToProduct(Long productId, List<ProductImageCreateRequest> requests) {
        // 1. Product 조회 (책임: ProductService)
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. ProductImageService에 Product 엔티티를 인자로 전달하여 호출 (단방향)
        return productImageCommandServiceApi.createProductImages(product, requests);
    }
}
