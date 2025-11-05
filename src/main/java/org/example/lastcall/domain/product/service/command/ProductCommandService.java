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
import org.example.lastcall.domain.product.service.validator.ProductValidator;
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
public class ProductCommandService implements ProductCommandServiceApi {
    private final ProductRepository productRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserServiceApi userServiceApi;
    private final ProductImageRepository productImageRepository;
    private final ProductValidator productValidator;
    private final ProductImageService productImageService;
    private final S3Service s3Service;

    //상품 등록
    public ProductResponse createProduct(AuthUser authuser, ProductCreateRequest request) {
        User user = userServiceApi.findById(authuser.userId());
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    //이미지 등록 (여러 장 등록)
    public List<ProductImageResponse> createProductImages(Long productId,
                                                          List<ProductImageCreateRequest> requests,
                                                          List<MultipartFile> images,
                                                          AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        productValidator.checkOwnership(product, authUser);
        productValidator.validateImageCount(requests);
        productValidator.validateThumbnailConsistencyForCreate(productId, requests);

        // 공통 메서드 호출
        List<ProductImage> imagesToSave = uploadAndGenerateImages(product, requests, images, productId);
        // DB 저장
        return productImageRepository.saveAll(imagesToSave).stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    //상품 정보 수정
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidator.checkOwnership(product, authUser);

        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());

        return ProductResponse.from(product);
    }

    //상품 수정 시 이미지 추가
    public List<ProductImageResponse> appendProductImages(Long productId,
                                                          List<ProductImageCreateRequest> requests,
                                                          List<MultipartFile> images,
                                                          AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(product.getId());
        productValidator.checkOwnership(product, authUser);

        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(product.getId());

        // 공통 메서드 호출
        List<ProductImage> newImages = uploadAndGenerateImages(product, requests, images, productId);

        //전체 이미지 합치기
        List<ProductImage> allImages = new ArrayList<>(existingImages);
        allImages.addAll(newImages);

        productValidator.validateImageCount(allImages);
        productValidator.validateThumbnailConsistencyForAppend(allImages);

        return productImageRepository.saveAll(newImages).stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    //썸네일 이미지 변경
    public List<ProductImageResponse> updateThumbnailImage(Long productId, Long newThumbnailImageId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidator.checkOwnership(product, authUser);

        //기존 썸네일 찾기
        Optional<ProductImage> currentThumbnail = productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);

        //기존 썸네일이 있으면 일반 이미지로 변경하기
        currentThumbnail.ifPresent(image -> image.updateImageType(ImageType.DETAIL));

        //새로 선택한 이미지를 썸네일로 변경
        ProductImage newThumbnail = productImageRepository.findById(newThumbnailImageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        newThumbnail.updateImageType(ImageType.THUMBNAIL);

        // 변경 이후의 해당 상품의 전체 이미지 목록 반환
        List<ProductImage> productImages = productImageRepository.findAllByProductIdAndDeletedFalse(productId);

        // 썸네일 갯수 검사
        long thumbnailCount = productImageRepository.countByProductIdAndImageType(productId, ImageType.THUMBNAIL);
        if (thumbnailCount > 1) {
            throw new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED);
        }

        return ProductImageResponse.from(productImages);
    }

    //상품 삭제
    public void deleteProduct(Long productId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }
        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidator.checkOwnership(product, authUser);

        //상품에 연결된 이미지 전체 조회
        List<ProductImage> images = productImageRepository.findAllByProductIdAndDeletedFalse(productId);
        //s3에서 실제 이미지 삭제
        for (ProductImage image : images) {
            try {
                s3Service.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("[WARN] Failed to delete image from S3: {}", image.getImageUrl(), e);
            }
        }
        product.softDelete();
        //상품에 연결된 이미지까지 soft delete
        productImageRepository.softDeleteByProductId(productId);
    }

    //이미지 단건 삭제
    public void deleteProductImage(Long productId, Long imageId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDeleted()) {
            throw new BusinessException(ProductErrorCode.PRODUCT_DELETED);
        }

        auctionQueryServiceApi.validateAuctionStatusForModification(productId);
        productValidator.checkOwnership(product, authUser);

        ProductImage productImage = productImageRepository.findById(imageId).orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new BusinessException(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT);
        }
        boolean isThumbnail = productImage.getImageType() == ImageType.THUMBNAIL;

        //1.S3에서 실제 파일 삭제
        s3Service.deleteFile(productImage.getImageUrl());
        //2.DB에서 소프트딜리트
        productImage.softDelete();

        if (isThumbnail) {
            List<ProductImage> remainingImages = productImageRepository.findByProductIdAndDeletedFalseOrderByIdAsc(productId);
            if (!remainingImages.isEmpty()) {
                ProductImage newThumbnail = remainingImages.get(0);
                newThumbnail.updateImageType(ImageType.THUMBNAIL);
            }
        }
    }

    // 공통 메서드 : 이미지 업로드 + 해시 생성 + 중복 체크
    private List<ProductImage> uploadAndGenerateImages(Product product,
                                                       List<ProductImageCreateRequest> requests,
                                                       List<MultipartFile> images,
                                                       Long productId) {
        // 파일 해시 계산 + 중복 체크
        Map<MultipartFile, String> fileToHash = productImageService.validateAndGenerateHashes(images, productId);

        // S3 업로드 + ProductImage 객체 생성
        return IntStream.range(0, requests.size())
                .mapToObj(i -> productImageService.uploadAndCreateProductImage(
                        product,
                        requests.get(i),
                        images.get(i),
                        fileToHash.get(images.get(i)),
                        productId))
                .toList();
    }
}
