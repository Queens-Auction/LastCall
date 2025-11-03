package org.example.lastcall.domain.product.sevice.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.sevice.ProductValidator;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService implements ProductCommandServiceApi {
    private final ProductRepository productRepository;
    private final AuctionQueryServiceApi auctionQueryServiceApi;
    private final UserServiceApi userServiceApi;
    private final ProductImageRepository productImageRepository;
    private final ProductValidator productValidator;
    private final S3Service s3Service;

    //상품 등록
    public ProductResponse createProduct(AuthUser authuser, ProductCreateRequest request) {
        User user = userServiceApi.findById(authuser.userId());
        Product product = Product.of(user, request.getName(), request.getCategory(), request.getDescription());
        Product savedProduct = productRepository.save(product);

        return ProductResponse.from(savedProduct);
    }

    //이미지 등록 (여러 장 등록)
    public List<ProductImageResponse> createProductImages(
            Long productId,
            List<ProductImageCreateRequest> requests,
            AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }

        productValidator.validateImageCount(requests);
        productValidator.validateDuplicateUrls(requests, product.getId());

        List<ProductImage> images = requests.stream()
                .map(req -> {
                    String imageUrl = s3Service.uploadToS3(req.getMultipartFile(), "products/" + productId);
                    ImageType imageType = (req.getIsThumbnail() != null && req.getIsThumbnail())
                            ? ImageType.THUMBNAIL
                            : ImageType.DETAIL;
                    return ProductImage.of(product, imageType, imageUrl);
                })
                .toList();

        productValidator.validateThumbnailConsistency(productId, images);

        List<ProductImage> savedImages = productImageRepository.saveAll(images);

        return savedImages.stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    //상품 정보 수정
    public ProductResponse updateProduct(Long productId, ProductUpdateRequest request, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        auctionQueryServiceApi.validateAuctionStatusForModification(productId);

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }

        product.updateProducts(request.getName(), request.getCategory(), request.getDescription());

        return ProductResponse.from(product);
    }

    //상품 수정 시 이미지 추가
    public List<ProductImageResponse> appendProductImages(
            Long productId,
            List<ProductImageCreateRequest> requests,
            AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        auctionQueryServiceApi.validateAuctionStatusForModification(product.getId());

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }

        //기존 이미지 불러오기
        List<ProductImage> existingImages = productImageRepository.findAllByProductIdAndDeletedFalse(product.getId());

        //중복 URL 체크
        productValidator.validateDuplicateUrls(requests, productId);

        //새 이미지 객체생성
        List<ProductImage> images = requests.stream()
                .map(req -> {
                    String imageUrl = s3Service.uploadToS3(req.getMultipartFile(), "products/" + productId);
                    ImageType imageType = (req.getIsThumbnail() != null && req.getIsThumbnail())
                            ? ImageType.THUMBNAIL
                            : ImageType.DETAIL;
                    return ProductImage.of(product, imageType, imageUrl);
                })
                .toList();

        //전체 이미지 합치기
        List<ProductImage> allImages = new ArrayList<>();
        allImages.addAll(existingImages);
        allImages.addAll(images);

        //총 이미지 갯수 검증
        productValidator.validateImageCount(allImages);

        //썸네일 한 개만 유지
        productValidator.validateThumbnailConsistency(productId, allImages);

        //새 이미지들만 저장
        List<ProductImage> savedImages = productImageRepository.saveAll(images);

        return savedImages.stream()
                .map(ProductImageResponse::from)
                .toList();
    }

    //썸네일 이미지 변경
    public List<ProductImageResponse> updateThumbnailImage(Long productId, Long newThumbnailImageId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        auctionQueryServiceApi.validateAuctionStatusForModification(productId);

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }

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
        auctionQueryServiceApi.validateAuctionStatusForModification(productId);

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }


        product.softDelete();

        //상품에 연결된 이미지까지 soft delete
        productImageRepository.softDeleteByProductId(productId);
    }

    //이미지 단건 삭제
    public void deleteProductImage(Long productId, Long imageId, AuthUser authUser) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        auctionQueryServiceApi.validateAuctionStatusForModification(productId);

        // 소유자 확인
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED); // 접근 거부
        }

        ProductImage productImage = productImageRepository.findById(imageId).orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new BusinessException(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT);
        }
        boolean isThumbnail = productImage.getImageType() == ImageType.THUMBNAIL;

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
