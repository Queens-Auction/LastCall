package org.example.lastcall.domain.product.sevice.query;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService implements ProductQueryServiceApi {
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    //내 상품 전체 조회(상품 아이디와 상품명만 조회 : 내 상품 관리용 상품 전체 조회)
    public PageResponse<ProductReadAllResponse> getAllMyProduct(AuthUser authuser, int page, int size) {
        Page<Product> products = productRepository.findAllByUserIdAndDeletedFalse(
                authuser.userId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        //대표 이미지들 한 번에 조회
        List<ProductImage> thumbnails = productImageRepository.findAllThumbnailsByProductIds(productIds);

        //productId -> imageUrl 매핑
        Map<Long, String> thumbnailUrlMap = thumbnails.stream()
                .collect(Collectors.toMap(pi -> pi.getProduct().getId(), ProductImage::getImageUrl));
        return ProductReadAllResponse.from(products, thumbnailUrlMap);
    }

    //상품 단건 조회
    public ProductReadOneResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImageResponse> images = productImageRepository.findAllByProductIdAndDeletedFalse(productId).stream()
                .map(ProductImageResponse::from)
                .toList();

        return ProductReadOneResponse.from(product, images);
    }

    //대표이미지 조회
    @Override
    @Transactional(readOnly = true)
    public ProductImageResponse readThumbnailImage(Long productId) {
        ProductImage thumbnailImage = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.THUMBNAIL_NOT_FOUND));
        return ProductImageResponse.from(thumbnailImage);
    }

    //상품별 이미지 전체 조회
    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> readAllProductImage(Long productId) {
        List<ProductImage> productImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);
        return ProductImageResponse.from(productImages);
    }

    //상품 조회
    @Override
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    //상품 소유자 검증
    @Override
    public void validateProductOwner(Long productId, Long userId) {
        Product product = productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!Objects.equals(product.getUser().getId(), userId)) {
            throw new BusinessException(ProductErrorCode.UNAUTHORIZED_PRODUCT_OWNER);
        }
    }
}
