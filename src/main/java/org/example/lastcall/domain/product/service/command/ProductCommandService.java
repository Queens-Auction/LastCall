package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.service.validator.ProductValidatorService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductCommandService {
    private final ProductRepository productRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserQueryServiceApi userQueryServiceApi;
    private final ProductImageRepository productImageRepository;
    private final ProductValidatorService productValidatorService;
    private final ProductImageService productImageService;
    private final S3Service s3Service;

    public ProductResponse createProduct(AuthUser authuser, ProductCreateRequest request) {
        User user = userQueryServiceApi.findById(authuser.userId());

        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    public List<ProductImageResponse> createProductImages(
            Long productId,
            List<MultipartFile> images,
            AuthUser authUser) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        auctionQueryServiceApi.validateAuctionStatusForModification(product.getId());
        productValidatorService.checkOwnership(product, authUser);

        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(product.getId());

        List<ProductImage> newImages = productImageService.uploadAndGenerateDetailImages(product, images, productId);

        List<ProductImage> allImages = new ArrayList<>(existingImages);
        allImages.addAll(newImages);

        productValidatorService.validateImageCount(allImages);

        return productImageRepository.saveAll(newImages).stream()
                .map(image -> {
                    String url = s3Service.generateImageUrl(image.getImageKey());
                    return ProductImageResponse.from(image, url);
                })
                .toList();
    }

    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, AuthUser authUser) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());

        return ProductResponse.from(product);
    }

    public List<ProductImageResponse> setThumbnailImage(Long productId, Long newThumbnailImageId, AuthUser authUser) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        Optional<ProductImage> currentThumbnail = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);
        currentThumbnail.ifPresent(image -> image.updateImageType(ImageType.DETAIL));

        ProductImage newThumbnail = productImageRepository.findByIdAndDeletedFalse(newThumbnailImageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));

        newThumbnail.updateImageType(ImageType.THUMBNAIL);

        List<ProductImage> productImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);

        return productImages.stream()
                .map(image -> {
                    String url = s3Service.generateImageUrl(image.getImageKey());
                    return ProductImageResponse.from(image, url);
                })
                .toList();
    }

    public void deleteProduct(Long productId, AuthUser authUser) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        List<ProductImage> images = productImageRepository.findAllByProductIdAndDeletedFalse(productId);

        for (ProductImage image : images) {
            s3Service.deleteFile(image.getImageKey());
        }

        product.softDelete();

        productImageRepository.softDeleteByProductId(productId);
    }

    public void deleteProductImage(Long productId, Long imageId, AuthUser authUser) {
        Product product = productRepository.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        ProductImage productImage = productImageRepository.findByIdAndDeletedFalse(imageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));

        if (!productImage.getProduct().getId().equals(productId)) {
            throw new BusinessException(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT);
        }

        boolean isThumbnail = productImage.getImageType() == ImageType.THUMBNAIL;

        s3Service.deleteFile(productImage.getImageKey());
        productImage.softDelete();

        if (isThumbnail) {
            List<ProductImage> remainingImages = productImageRepository.findByProductIdAndDeletedFalseOrderByIdAsc(productId);

            if (!remainingImages.isEmpty()) {
                ProductImage newThumbnail = remainingImages.get(0);
                newThumbnail.updateImageType(ImageType.THUMBNAIL);
            }
        }
    }
}
