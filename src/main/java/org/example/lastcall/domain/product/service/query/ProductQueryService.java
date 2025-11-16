package org.example.lastcall.domain.product.service.query;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.service.command.S3Service;
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
    private final S3Service s3Service;

    public PageResponse<ProductReadAllResponse> getAllMyProducts(AuthUser authuser, int page, int size) {
        Page<Product> products = productRepository.findAllByUserIdAndDeletedFalse(
                authuser.userId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<Long, String> thumbnailUrlMap = getThumbnailUrlMap(products);

        return ProductReadAllResponse.from(products, thumbnailUrlMap);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductImageResponse findThumbnailImage(Long productId) {
        ProductImage thumbnailImage = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.THUMBNAIL_NOT_FOUND));

        return ProductImageResponse.from(thumbnailImage, s3Service);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductImageResponse> findAllProductImage(Long productId) {
        List<ProductImage> productImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);

        return ProductImageResponse.from(productImages, s3Service);
    }

    @Override
    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public Product validateProductOwner(Long productId, Long userId) {
        Product product = productRepository.findByIdWithUser(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (!Objects.equals(product.getUser().getId(), userId)) {
            throw new BusinessException(ProductErrorCode.UNAUTHORIZED_PRODUCT_OWNER);
        }
        return product;
    }

    private Map<Long, String> getThumbnailUrlMap(Page<Product> products) {
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .toList();

        return productImageRepository.findAllThumbnailsByProductIds(productIds)
                .stream()
                .collect(Collectors.toMap(
                        pi -> pi.getProduct().getId(),
                        pi -> s3Service.generateImageUrl(pi.getImageKey())));
    }
}
