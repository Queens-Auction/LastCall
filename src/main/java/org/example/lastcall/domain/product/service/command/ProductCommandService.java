package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
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
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductCommandService {
    private final ProductRepository productRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserServiceApi userServiceApi;
    private final ProductImageRepository productImageRepository;
    private final ProductValidatorService productValidatorService;
    private final ProductImageService productImageService;
    private final S3Service s3Service;

    public ProductResponse createProduct(AuthUser authuser, ProductCreateRequest request) {
        User user = userServiceApi.findById(authuser.userId());
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    public List<ProductImageResponse> createProductImages(Long productId,
                                                          List<ProductImageCreateRequest> requests,
                                                          List<MultipartFile> images,
                                                          AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        productValidatorService.checkOwnership(product, authUser);
        productValidatorService.validateImageCount(requests);
        productValidatorService.validateThumbnailConsistencyForCreate(productId, requests);

        List<ProductImage> imagesToSave = uploadAndGenerateImages(product, requests, images, productId);

        return productImageRepository.saveAll(imagesToSave).stream()
                .map(image -> ProductImageResponse.from(image, s3Service))
                .toList();
    }

    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());

        return ProductResponse.from(product);
    }

    public List<ProductImageResponse> appendProductImages(Long productId,
                                                          List<MultipartFile> images,
                                                          AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(product.getId());
        productValidatorService.checkOwnership(product, authUser);

        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(product.getId());

        List<ProductImage> newImages = uploadAndGenerateDetailImages(product, images, productId);

        List<ProductImage> allImages = new ArrayList<>(existingImages);
        allImages.addAll(newImages);

        productValidatorService.validateImageCount(allImages);
        productValidatorService.validateThumbnailConsistencyForAppend(allImages);

        return productImageRepository.saveAll(newImages).stream()
                .map(image -> ProductImageResponse.from(image, s3Service))
                .toList();
    }

    public List<ProductImageResponse> updateThumbnailImage(Long productId, Long newThumbnailImageId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        Optional<ProductImage> currentThumbnail = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);

        currentThumbnail.ifPresent(image -> image.updateImageType(ImageType.DETAIL));

        ProductImage newThumbnail = productImageRepository.findById(newThumbnailImageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        newThumbnail.updateImageType(ImageType.THUMBNAIL);

        List<ProductImage> productImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);

        long thumbnailCount = productImageRepository.countByProductIdAndImageType(productId, ImageType.THUMBNAIL);
        if (thumbnailCount > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        return ProductImageResponse.from(productImages, s3Service);
    }

    public void deleteProduct(Long productId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidatorService.checkOwnership(product, authUser);

        ProductImage productImage = productImageRepository.findById(imageId)
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

    public List<ProductImage> uploadAndGenerateImages(Product product,
                                                      List<ProductImageCreateRequest> requests,
                                                      List<MultipartFile> images,
                                                      Long productId) {
        Map<MultipartFile, String> fileToHash = productImageService.validateAndGenerateHashes(images, productId);

        return IntStream.range(0, requests.size())
                .mapToObj(i -> productImageService.uploadAndCreateProductImage(
                        product,
                        requests.get(i),
                        images.get(i),
                        fileToHash.get(images.get(i)),
                        productId))
                .toList();
    }

    public List<ProductImage> uploadAndGenerateDetailImages(Product product,
                                                            List<MultipartFile> images,
                                                            Long productId) {
        Map<MultipartFile, String> fileToHash = productImageService.validateAndGenerateHashes(images, productId);

        return images.stream()
                .map(image -> productImageService.uploadAndCreateProductImage(
                        product,
                        image,
                        fileToHash.get(image),
                        productId,
                        ImageType.DETAIL))
                .toList();
    }
}
