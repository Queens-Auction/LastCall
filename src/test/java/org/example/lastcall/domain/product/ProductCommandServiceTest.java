package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.service.query.AuctionQueryServiceApi;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.service.command.ProductCommandService;
import org.example.lastcall.domain.product.service.command.ProductImageService;
import org.example.lastcall.domain.product.service.command.S3Service;
import org.example.lastcall.domain.product.service.validator.ProductValidatorService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.service.query.UserQueryServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserQueryServiceApi userQueryServiceApi;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductValidatorService productValidatorService;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private S3Service s3Service;

    @Mock
    private AuctionQueryServiceApi auctionQueryServiceApi;

    @InjectMocks
    private ProductCommandService productCommandService;

    private Product product;
    private Product product2;

    @BeforeEach
    void setUp() {
        User user = User.of(
                UUID.randomUUID(),
                "testUser",
                "tester",
                "test123@example.com",
                "encoded-Password1!",
                "Seoul",
                "12345",
                "Apt 101",
                "010-0000-0000",
                Role.USER);

        product = Product.of(
                user,
                "제가 그린 기린 그림",
                Category.ART_PAINTING,
                "제가 그린 기린 그림입니다. 저는 여섯살 때부터 신바람 영재 미술 교실을 다닌 바가 있으며 계속 취미 생활을 유지중입니다.");

        product2 = Product.of(
                user,
                "짱돌",
                Category.HOME_DECOR,
                "애완 돌을 키워보세요. 당신의 모든 고민도 들어드립니다. 절대 소문 안냄");

        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product2, "id", 2L);
    }

    @Test
    @DisplayName("상품 등록 성공")
    void createProduct_상품_등록에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        ProductCreateRequest request = new ProductCreateRequest(
                product.getName(),
                product.getCategory(),
                product.getDescription());

        when(userQueryServiceApi.findById(authUser.userId())).thenReturn(product.getUser());
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productCommandService.createProduct(authUser, request);

        assertNotNull(response);
        assertEquals(product.getId(), response.getId());
        assertEquals(product.getName(), response.getName());
        assertEquals(product.getCategory(), response.getCategory());
        assertEquals(product.getDescription(), response.getDescription());

        verify(userQueryServiceApi, times(1)).findById(authUser.userId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 이미지 등록 성공")
    void createProductImages_상품_이미지_등록에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true),
                new ProductImageCreateRequest(false));

        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "dummy content".getBytes()),
                new MockMultipartFile("file1", "file2.jpg", "image/jpeg", "dummy content".getBytes()));

        doNothing().when(productValidatorService).checkOwnership(product, authUser);
        doNothing().when(productValidatorService).validateImageCount(requests);
        doNothing().when(productValidatorService).validateThumbnailConsistencyForCreate(product.getId(), requests);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        List<ProductImage> savedImages = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            ProductImage img = ProductImage.of(
                    product,
                    ImageType.DETAIL,
                    "dummy-key-" + i + ".jpg",
                    "dummy-hash-" + i);
            savedImages.add(img);
        }

        when(productImageRepository.saveAll(anyList())).thenReturn(savedImages);

        List<ProductImageResponse> responses = productCommandService.createProductImages(
                product.getId(),
                requests,
                images,
                authUser
        );

        assertNotNull(responses);
        assertEquals(savedImages.size(), responses.size());

        for (int i = 0; i < responses.size(); i++) {
            assertEquals(savedImages.get(i).getId(), responses.get(i).getId());
            assertEquals(savedImages.get(i).getImageType(), responses.get(i).getImageType());
        }

        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
        verify(productValidatorService, times(1)).validateImageCount(requests);
        verify(productValidatorService, times(1)).validateThumbnailConsistencyForCreate(product.getId(), requests);
        verify(productRepository, times(1)).findById(product.getId());
        verify(productImageRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("상품이 없으면 예외 발생")
    void createProductImages_상품이_없으면_예외가_발생한다() {
        Long productId = 1L;
        AuthUser authUser = new AuthUser(1L, "user", "USER");
        List<ProductImageCreateRequest> requests = List.of(new ProductImageCreateRequest(true));
        List<MultipartFile> images = List.of(new MockMultipartFile("file", "file.jpg", "image/jpeg", "data".getBytes()));

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.createProductImages(productId, requests, images, authUser));

        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("삭제된 상품이면 예외 발생")
    void createProductImages_삭제된_상품이면_예외가_발생한다() {
        Long productId = 1L;
        AuthUser authUser = new AuthUser(1L, "user", "USER");
        List<ProductImageCreateRequest> requests = List.of(new ProductImageCreateRequest(true));
        List<MultipartFile> images = List.of(new MockMultipartFile("file,", "file.jpg", "image/jpeg", "data".getBytes()));

        Product deletedProduct = Product.of(product.getUser(), "name", Category.ART_PAINTING, "desc");
        deletedProduct.softDelete();

        when(productRepository.findById(productId)).thenReturn(Optional.of(deletedProduct));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.createProductImages(productId, requests, images, authUser));
        assertEquals(ProductErrorCode.PRODUCT_DELETED, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미지 개수 초과 시 BusinessException 발생")
    void createProductImages_이미지_개수_초과_시_예외가_발생한다() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        List<ProductImageCreateRequest> requests = List.of(new ProductImageCreateRequest(true));
        List<MultipartFile> images = List.of(new MockMultipartFile("file", "file.jpg", "image/jpeg", "data".getBytes()));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        doThrow(new BusinessException(ProductErrorCode.TOO_MANY_IMAGES))
                .when(productValidatorService).validateImageCount(requests);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.createProductImages(productId, requests, images, authUser));

        assertEquals(ProductErrorCode.TOO_MANY_IMAGES, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 정보 수정 성공")
    void updateProduct_상품_정보_수정에_성공한다() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                Category.HOME_DECOR,
                "수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidatorService).checkOwnership(product, authUser);

        ProductResponse response = productCommandService.updateProduct(productId, request, authUser);

        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getCategory(), response.getCategory());
        assertEquals(request.getDescription(), response.getDescription());

        verify(productRepository, times(1)).findById(productId);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
    }

    @Test
    @DisplayName("경매 진행 중인 상품 수정 시 예외 발생")
    void updateProduct_경매가_진행중이면_예외가_발생한다() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        ProductUpdateRequest request = new ProductUpdateRequest("new name", Category.HOME_DECOR, "new description");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        doThrow(new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION))
                .when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateProduct(productId, request, authUser));

        assertEquals(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION, exception.getErrorCode());

        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidatorService, never()).checkOwnership(any(), any());
    }

    @Test
    @DisplayName("상품 수정 시 소유권 검증 실패")
    void updateProduct_소유권_검증_실패_시_예외가_발생한다() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        ProductUpdateRequest request = new ProductUpdateRequest("new name", Category.HOME_DECOR, "new describe");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);

        doThrow(new BusinessException(ProductErrorCode.ACCESS_DENIED))
                .when(productValidatorService).checkOwnership(product, authUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateProduct(productId, request, authUser));

        assertEquals(ProductErrorCode.ACCESS_DENIED, exception.getErrorCode());

        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
    }

    @Test
    @DisplayName("상품 이미지 추가 성공")
    void appendProductImages_상품_이미지_추가에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        MockMultipartFile mockFile1 = new MockMultipartFile(
                "image", "test1.jpg", "image/jpeg", "dummy1".getBytes()
        );
        MockMultipartFile mockFile2 = new MockMultipartFile(
                "image", "test2.jpg", "image/jpeg", "dummy2".getBytes()
        );
        List<MultipartFile> inputImages = List.of(mockFile1, mockFile2);

        List<ProductImage> existingImages = List.of(
                ProductImage.of(product, ImageType.THUMBNAIL, "existing-1.jpg", "hash1"),
                ProductImage.of(product, ImageType.DETAIL, "existing-2.jpg", "hash2")
        );

        ProductImage newImage1 = ProductImage.of(product, ImageType.DETAIL, "test1.jpg", "hash1");
        ProductImage newImage2 = ProductImage.of(product, ImageType.DETAIL, "test2.jpg", "hash2");

        when(productRepository.findByIdAndDeletedFalse(product.getId())).thenReturn(Optional.of(product));
        when(productImageRepository.findAllByProductIdAndDeletedFalse(product.getId())).thenReturn(existingImages);
        when(productImageService.uploadAndGenerateDetailImages(product, inputImages, product.getId()))
                .thenReturn(List.of(newImage1, newImage2));
        when(s3Service.generateImageUrl(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(product.getId());
        doNothing().when(productValidatorService).checkOwnership(product, authUser);
        doNothing().when(productValidatorService).validateImageCount(anyList());

        when(productImageRepository.saveAll(any())).thenReturn(List.of(newImage1, newImage2));

        List<ProductImageResponse> responses = productCommandService.appendProductImages(product.getId(), inputImages, authUser);

        assertEquals(2, responses.size());
        assertEquals("test1.jpg", responses.get(0).getImageUrl());
        assertEquals("test2.jpg", responses.get(1).getImageUrl());

        verify(productRepository, times(1)).findByIdAndDeletedFalse(product.getId());
        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
        verify(productValidatorService, times(1)).validateImageCount(anyList());
        verify(productImageRepository, times(1)).saveAll(any());
        verify(productImageService).uploadAndGenerateDetailImages(product, inputImages, product.getId());
    }

    @Test
    @DisplayName("경매 진행 중인 이미지 추가 시 예외 발생")
    void appendProductImages_경매_진행중이면_예외가_발생한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true));

        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "data".getBytes()));

        when(productRepository.findByIdAndDeletedFalse(product.getId())).thenReturn(Optional.of(product));

        doThrow(new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION))
                .when(auctionQueryServiceApi).validateAuctionStatusForModification(product.getId());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.appendProductImages(product.getId(), images, authUser));

        assertEquals(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION, exception.getErrorCode());

        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(product.getId());
        verify(productValidatorService, never()).checkOwnership(any(), any());
    }

    @Test
    @DisplayName("썸네일 이미지 변경 성공")
    void updateThumbnailImage_썸네일_이미지_변경에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();
        Long newThumbnailId = 10L;

        ProductImage currentThumbnail = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash-thumb");
        ProductImage newThumbnail = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash-detail");

        List<ProductImage> allImages = new ArrayList<>();
        allImages.add(currentThumbnail);
        allImages.add(newThumbnail);

        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidatorService).checkOwnership(product, authUser);

        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL))
                .thenReturn(Optional.of(currentThumbnail));
        when(productImageRepository.findByIdAndDeletedFalse(newThumbnailId))
                .thenReturn(Optional.of(newThumbnail));
        when(productImageRepository.findAllByProductIdAndDeletedFalse(productId))
                .thenReturn(allImages);
        when(s3Service.generateImageUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProductImageResponse> responses = productCommandService.updateThumbnailImage(productId, newThumbnailId, authUser);

        assertNotNull(responses);
        assertEquals(allImages.size(), responses.size());
        assertEquals(ImageType.DETAIL, currentThumbnail.getImageType());
        assertEquals(ImageType.THUMBNAIL, newThumbnail.getImageType());

        verify(productRepository, times(1)).findByIdAndDeletedFalse(productId);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
        verify(productImageRepository, times(1))
                .findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);
        verify(productImageRepository, times(1)).findByIdAndDeletedFalse(newThumbnailId);
        verify(productImageRepository, times(1)).findAllByProductIdAndDeletedFalse(productId);
    }

    @Test
    @DisplayName("썸네일 변경 시 새로운 이미지 없음 예외 발생")
    void updateThumbnailImage_새로운_이미지가_없을_시_예외가_발생한다() {
        Long productId = product.getId();
        Long newThumbnailId = 100L;
        AuthUser authUSer = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        when(productRepository.findByIdAndDeletedFalse(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidatorService).checkOwnership(product, authUSer);
        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL))
                .thenReturn(Optional.empty());
        when(productImageRepository.findByIdAndDeletedFalse(newThumbnailId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateThumbnailImage(productId, newThumbnailId, authUSer));

        assertEquals(ProductErrorCode.IMAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 삭제 성공")
    void deleteProduct_상품_삭제에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();

        ProductImage image1 = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash1");
        ProductImage image2 = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash2");
        List<ProductImage> images = List.of(image1, image2);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidatorService).checkOwnership(product, authUser);

        when(productImageRepository.findAllByProductIdAndDeletedFalse(productId)).thenReturn(images);
        doNothing().when(s3Service).deleteFile(anyString());
        doNothing().when(productImageRepository).softDeleteByProductId(productId);

        productCommandService.deleteProduct(productId, authUser);

        assertTrue(product.isDeleted());

        verify(s3Service, times(images.size())).deleteFile(anyString());
        verify(s3Service).deleteFile("thumb.jpg");
        verify(s3Service).deleteFile("detail.jpg");

        verify(productImageRepository, times(1)).softDeleteByProductId(productId);
        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
    }

    @Test
    @DisplayName("이미지 단건 삭제 성공")
    void deleteProductImage_이미지_단건_삭제에_성공한다() {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();
        Long imageId = 10L;

        ProductImage thumbnailImage = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash-thumb");
        ProductImage detailImage = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash-detail");

        List<ProductImage> remainingImages = List.of(detailImage);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidatorService).checkOwnership(product, authUser);

        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(thumbnailImage));
        when(productImageRepository.findByProductIdAndDeletedFalseOrderByIdAsc(productId))
                .thenReturn(remainingImages);
        doNothing().when(s3Service).deleteFile(thumbnailImage.getImageKey());

        productCommandService.deleteProductImage(productId, imageId, authUser);

        assertTrue(thumbnailImage.isDeleted());

        verify(s3Service, times(1)).deleteFile(thumbnailImage.getImageKey());

        assertEquals(ImageType.THUMBNAIL, remainingImages.get(0).getImageType());

        verify(productValidatorService, times(1)).checkOwnership(product, authUser);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
    }

    @Test
    @DisplayName("이미지가 상품에 속하지 않음")
    void deleteProductImage_이미지가_상품에_속하지않을_시_예외가_발생한다() {
        Long productId = product.getId();
        Long imageId = 2L;
        AuthUser authUSer = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        ProductImage wrongImage = ProductImage.of(product2, ImageType.THUMBNAIL, "worngImage.jpg", "hash");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(wrongImage));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.deleteProductImage(productId, imageId, authUSer));

        assertEquals(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT, exception.getErrorCode());
    }
}
