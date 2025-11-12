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
import org.example.lastcall.domain.product.service.validator.ProductValidator;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    private UserServiceApi userServiceApi;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductValidator productValidator;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private S3Service s3Service;

    @Mock
    private AuctionQueryServiceApi auctionQueryServiceApi;

    @InjectMocks
    private ProductCommandService productCommandService;

    private Long productId;
    private Product product;
    private Product product2;

    @BeforeEach
    void setUp() {
        productId = 1L;
        User user = User.createForSignUp(
                UUID.randomUUID(),
                "testUser",
                "tester",
                "test123@example.com",
                "encoded-Password1!",
                "Seoul",
                "12345",
                "Apt 101",
                "010-0000-0000",
                Role.USER
        );

        product = Product.of(
                user,
                "제가 그린 기린 그림",
                Category.ART_PAINTING,
                "제가 그린 기린 그림입니다. 저는 여섯살 때부터 신바람 영재 미술 교실을 다닌 바가 있으며 계속 취미 생활을 유지중입니다."
        );

        product2 = Product.of(
                user,
                "짱돌",
                Category.HOME_DECOR,
                "애완 돌을 키워보세요. 당신의 모든 고민도 들어드립니다. 절대 소문 안냄");

        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product2, "id", 2L);
    }

    @Test
    @DisplayName("상품 등록 성공 - createProduct()")
    void createProduct_success() {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        ProductCreateRequest request = new ProductCreateRequest(
                product.getName(),
                product.getCategory(),
                product.getDescription());

        //userServiceApi.findById()가 호출되면 product의 user 반환
        when(userServiceApi.findById(authUser.userId())).thenReturn(product.getUser());

        //productRepository.save()가 호출되면 product 반 (id는 이미 세팅되어 있음)
        when(productRepository.save(any(Product.class))).thenReturn(product);

        //when
        ProductResponse response = productCommandService.createProduct(authUser, request);

        //then
        assertNotNull(response);
        assertEquals(product.getId(), response.getId());
        assertEquals(product.getName(), response.getName());
        assertEquals(product.getCategory(), response.getCategory());
        assertEquals(product.getDescription(), response.getDescription());

        //verify 호출 여부
        verify(userServiceApi, times(1)).findById(authUser.userId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 이미지 등록 성공 - createProductImages()")
    void creatProductImages_success() throws Exception {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true),
                new ProductImageCreateRequest(false)
        );

        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "dummy content".getBytes()),
                new MockMultipartFile("file1", "file2.jpg", "image/jpeg", "dummy content".getBytes())
        );

        //validator void 메서드는 아무 동장 없이 패스
        doNothing().when(productValidator).checkOwnership(product, authUser);
        doNothing().when(productValidator).validateImageCount(requests);
        doNothing().when(productValidator).validateThumbnailConsistencyForCreate(product.getId(), requests);

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

        //s3Service는 ProductImageResponse.from에서 사용될 수 있으므로 간단히 Mocking
        //여기서는 실제 호출하지 않고 그냥 정상 ProductImageResponse 반환 가정
        //필요시 spy 사용 가능

        //when
        List<ProductImageResponse> responses = productCommandService.createProductImages(
                product.getId(),
                requests,
                images,
                authUser
        );

        //then
        assertNotNull(responses);
        assertEquals(savedImages.size(), responses.size());
        for (int i = 0; i < responses.size(); i++) {
            assertEquals(savedImages.get(i).getId(), responses.get(i).getId());
            assertEquals(savedImages.get(i).getImageType(), responses.get(i).getImageType());
        }

        verify(productValidator, times(1)).checkOwnership(product, authUser);
        verify(productValidator, times(1)).validateImageCount(requests);
        verify(productValidator, times(1)).validateThumbnailConsistencyForCreate(product.getId(), requests);
        verify(productRepository, times(1)).findById(product.getId());
        verify(productImageRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("상품이 없으면 예외 발생 - createProductImages()")
    void createProductImage_productNotFound() {
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
    @DisplayName("삭제된 상품이면 예외 발생 - createProductImages()")
    void createProductImages_productDeleted() {
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
    @DisplayName("Validator 예외 발생 - createProductImages()")
    void createProductImages_validatorException() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        List<ProductImageCreateRequest> requests = List.of(new ProductImageCreateRequest(true));
        List<MultipartFile> images = List.of(new MockMultipartFile("file", "file.jpg", "image/jpeg", "data".getBytes()));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        //validator 에서 예외 발생
        doThrow(new BusinessException(ProductErrorCode.TOO_MANY_IMAGES))
                .when(productValidator).validateImageCount(requests);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.createProductImages(productId, requests, images, authUser));

        assertEquals(ProductErrorCode.TOO_MANY_IMAGES, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 정보 수정 성공 - updateProduct()")
    void updateProduct_success() {
        //given
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        ProductUpdateRequest request = new ProductUpdateRequest(
                "수정된 상품명",
                Category.HOME_DECOR,
                "수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명 수정된 상품 설명"
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUser);

        //when
        ProductResponse response = productCommandService.updateProduct(productId, request, authUser);

        //then
        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getCategory(), response.getCategory());
        assertEquals(request.getDescription(), response.getDescription());

        verify(productRepository, times(1)).findById(productId);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidator, times(1)).checkOwnership(product, authUser);
    }

    @Test
    @DisplayName("상품 수정 시 경매 상태 검증 실패 - updateProduct()")
    void updateProduct_auctionStatusExceeption() {
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
        verify(productValidator, never()).checkOwnership(any(), any());
    }

    @Test
    @DisplayName("상품 수정 시 소유권 검증 실패 - updateProduct()")
    void updateProduct_ownershipException() {
        Long productId = product.getId();
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        ProductUpdateRequest request = new ProductUpdateRequest("new name", Category.HOME_DECOR, "new describe");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        //auction 검증 통과
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);

        //validator에서 예외 발생
        doThrow(new BusinessException(ProductErrorCode.ACCESS_DENIED))
                .when(productValidator).checkOwnership(product, authUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateProduct(productId, request, authUser));

        assertEquals(ProductErrorCode.ACCESS_DENIED, exception.getErrorCode());

        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidator, times(1)).checkOwnership(product, authUser);
    }

    @Test
    @DisplayName("상품 이미지 추가 성공 - appendProductImages()")
    void appendProductImages_success() throws Exception {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true),
                new ProductImageCreateRequest(false)
        );

        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "dummy1".getBytes()),
                new MockMultipartFile("file2", "file2.jpg", "image/jpeg", "dummy2".getBytes())
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        // 기존 이미지 (이미 등록된 이미지)
        List<ProductImage> existingImages = List.of(
                ProductImage.of(product, ImageType.DETAIL, "existing-1.jpg", "hash1"),
                ProductImage.of(product, ImageType.DETAIL, "existing-2.jpg", "hash2")
        );

        when(productImageRepository.findAllByProductIdAndDeletedFalse(product.getId())).thenReturn(existingImages);

        //새로 추가할 이미지들
        List<ProductImage> newImages = List.of(
                ProductImage.of(product, ImageType.THUMBNAIL, "new-1.jpg", "hash3"),
                ProductImage.of(product, ImageType.DETAIL, "new-2.jpg", "hash4")
        );

        //uploadAndGenerateImages()는 private일 확률이 높으므로 실제 테스트에선 spy 또는 doReturn()으로 Mocking
        ProductCommandService spyService = Mockito.spy(productCommandService);
        doReturn(newImages).when(spyService).uploadAndGenerateImages(any(Product.class), anyList(), anyList(), anyLong());

        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(product.getId());
        doNothing().when(productValidator).checkOwnership(product, authUser);
        doNothing().when(productValidator).validateImageCount(anyList());
        doNothing().when(productValidator).validateThumbnailConsistencyForAppend(anyList());

        when(productImageRepository.saveAll(newImages)).thenReturn(newImages);

        //when
        List<ProductImageResponse> responses = spyService.appendProductImages(product.getId(), requests, images, authUser);

        //then
        assertNotNull(responses);
        assertEquals(newImages.size(), responses.size());
        for (ProductImageResponse response : responses) {
            //필요하다면 response의 썸네일 여부나 타입 등만 비교
            assertNotNull(response.getImageType());
        }

        //verify
        verify(productRepository, times(1)).findById(product.getId());
        verify(productImageRepository, times(1)).findAllByProductIdAndDeletedFalse(product.getId());
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(product.getId());
        verify(productValidator, times(1)).checkOwnership(product, authUser);
        verify(productValidator, times(1)).validateImageCount(anyList());
        verify(productValidator, times(1)).validateThumbnailConsistencyForAppend(anyList());
        verify(productImageRepository, times(1)).saveAll(newImages);
    }

    @Test
    @DisplayName("이미지 추가 시 경매 상태 검증 실패 - appendProductImages()")
    void appendProductImages_auctionStatusException() throws Exception {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true)
        );
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "data".getBytes())
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        doThrow(new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION))
                .when(auctionQueryServiceApi).validateAuctionStatusForModification(product.getId());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.appendProductImages(product.getId(), requests, images, authUser));

        assertEquals(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION, exception.getErrorCode());

        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(product.getId());
        verify(productValidator, never()).checkOwnership(any(), any());
    }

    @Test
    @DisplayName("이미지 추가 시 썸네일 일관성 예 - appendProductImages()")
    void appendProductImages_thumbnailConsistencyException() throws Exception {
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true)
        );
        List<MultipartFile> images = List.of(
                new MockMultipartFile("file1", "file1.jpg", "image/jpeg", "data".getBytes())
        );

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(product.getId());
        doNothing().when(productValidator).checkOwnership(product, authUser);

        List<ProductImage> newImages = List.of(ProductImage.of(product, ImageType.DETAIL, "new.jpg", "hash"));
        List<ProductImage> existingImages = List.of(ProductImage.of(product, ImageType.DETAIL, "exist", "hash2"));

        ProductCommandService spyService = Mockito.spy(productCommandService);
        doReturn(newImages).when(spyService).uploadAndGenerateImages(product, requests, images, product.getId());
        when(productImageRepository.findAllByProductIdAndDeletedFalse(product.getId())).thenReturn(existingImages);

        //validator에서 썸네일 일관성 예외 발생
        doThrow(new BusinessException(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED))
                .when(productValidator).validateThumbnailConsistencyForAppend(anyList());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> spyService.appendProductImages(product.getId(), requests, images, authUser));

        assertEquals(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("썸네일 이미지 변경 성공 - updateThumbnailImage()")
    void updateThumbnailImage_success() {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();
        Long newThumbnailId = 10L;

        ProductImage currentThumbnail = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash-thumb");
        ProductImage newThumbnail = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash-detail");

        List<ProductImage> allImages = new ArrayList<>();
        allImages.add(currentThumbnail);
        allImages.add(newThumbnail);

        //Mock repository & validator
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUser);

        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL))
                .thenReturn(Optional.of(currentThumbnail));
        when(productImageRepository.findById(newThumbnailId))
                .thenReturn(Optional.of(newThumbnail));
        when(productImageRepository.findAllByProductIdAndDeletedFalse(productId))
                .thenReturn(allImages);
        when(productImageRepository.countByProductIdAndImageType(productId, ImageType.THUMBNAIL))
                .thenReturn(1L);

        //Mock S3Service response if needed
        when(s3Service.generateImageUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        //when
        List<ProductImageResponse> responses = productCommandService.updateThumbnailImage(productId, newThumbnailId, authUser);

        //then
        assertNotNull(responses);
        assertEquals(allImages.size(), responses.size());

        //기존 썸네일은 DETAIL로 변경
        assertEquals(ImageType.DETAIL, currentThumbnail.getImageType());

        //새로운 썸네일은 THUMBNAIL로 변경
        assertEquals(ImageType.THUMBNAIL, newThumbnail.getImageType());

        //verify 호출
        verify(productRepository, times(1)).findById(productId);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
        verify(productValidator, times(1)).checkOwnership(product, authUser);
        verify(productImageRepository, times(1))
                .findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL);
        verify(productImageRepository, times(1)).findById(newThumbnailId);
        verify(productImageRepository, times(1)).findAllByProductIdAndDeletedFalse(productId);
        verify(productImageRepository, times(1)).countByProductIdAndImageType(productId, ImageType.THUMBNAIL);
    }

    @Test
    @DisplayName("썸네일 변경 시 새로운 이미지 없음 예외 발생 - updateThumbnailImage()")
    void updateThumbnailImage_newThumbnailNotFoundException() {
        Long productId = product.getId();
        Long newThumbnailId = 100L;
        AuthUser authUSer = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUSer);
        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL))
                .thenReturn(Optional.empty());
        when(productImageRepository.findById(newThumbnailId))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateThumbnailImage(productId, newThumbnailId, authUSer));

        assertEquals(ProductErrorCode.IMAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("썸네일 변경 시 중복 썸네일 존재 예외 발생 - updateThumbnailImage()")
    void updateThumbnailImage_multipleThumbnailException() {
        Long productId = product.getId();
        Long newThumbnailId = 100L;
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUser);

        ProductImage oldThumbnail = ProductImage.of(product, ImageType.THUMBNAIL, "old-thumb.jpg", "hash");
        ProductImage newThumbnail = ProductImage.of(product, ImageType.DETAIL, "newThumb.jpg", "hash2");

        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(productId, ImageType.THUMBNAIL))
                .thenReturn(Optional.of(oldThumbnail));
        when(productImageRepository.findById(newThumbnailId)).thenReturn(Optional.of(newThumbnail));
        when(productImageRepository.findAllByProductIdAndDeletedFalse(productId))
                .thenReturn(List.of(oldThumbnail, newThumbnail));
        when(productImageRepository.countByProductIdAndImageType(productId, ImageType.THUMBNAIL))
                .thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.updateThumbnailImage(productId, newThumbnailId, authUser));

        assertEquals(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 삭제 성공 - deleteProduct()")
    void deleteProduct_success() {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();

        ProductImage image1 = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash1");
        ProductImage image2 = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash2");
        List<ProductImage> images = List.of(image1, image2);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUser);

        when(productImageRepository.findAllByProductIdAndDeletedFalse(productId)).thenReturn(images);
        doNothing().when(s3Service).deleteFile(anyString());
        doNothing().when(productImageRepository).softDeleteByProductId(productId);

        //when
        productCommandService.deleteProduct(productId, authUser);

        //then
        assertTrue(product.isDeleted());

        //s3 삭제 호출 확인
        verify(s3Service, times(images.size())).deleteFile(anyString());
        verify(s3Service).deleteFile("thumb.jpg");
        verify(s3Service).deleteFile("detail.jpg");

        //soft delete 호출 확인
        verify(productImageRepository, times(1)).softDeleteByProductId(productId);

        //validator, auction service 호출 확인
        verify(productValidator, times(1)).checkOwnership(product, authUser);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
    }

    @Test
    @DisplayName("이미지 단건 삭제 성공 - deleteProductImage()")
    void deleteProductImage_success() {
        //given
        AuthUser authUser = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");
        Long productId = product.getId();
        Long imageId = 10L;

        ProductImage thumbnailImage = ProductImage.of(product, ImageType.THUMBNAIL, "thumb.jpg", "hash-thumb");
        ProductImage detailImage = ProductImage.of(product, ImageType.DETAIL, "detail.jpg", "hash-detail");

        List<ProductImage> remainingImages = List.of(detailImage);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionQueryServiceApi).validateAuctionStatusForModification(productId);
        doNothing().when(productValidator).checkOwnership(product, authUser);

        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(thumbnailImage));
        when(productImageRepository.findByProductIdAndDeletedFalseOrderByIdAsc(productId))
                .thenReturn(remainingImages);
        doNothing().when(s3Service).deleteFile(thumbnailImage.getImageKey());

        //when
        productCommandService.deleteProductImage(productId, imageId, authUser);

        //then
        //1.이미지 soft delete 확인
        assertTrue(thumbnailImage.isDeleted());

        //2.s3 삭제 호출 확인
        verify(s3Service, times(1)).deleteFile(thumbnailImage.getImageKey());

        //3.썸네일 재할당 확인
        assertEquals(ImageType.THUMBNAIL, remainingImages.get(0).getImageType());

        //4.validator & auction service 호출 확인
        verify(productValidator, times(1)).checkOwnership(product, authUser);
        verify(auctionQueryServiceApi, times(1)).validateAuctionStatusForModification(productId);
    }

    @Test
    @DisplayName("이미지가 상품에 속하지 않음 - image not belong to product")
    void deleteProductImage_imageNotBelongToProduct() {
        Long productId = product.getId();
        Long imageId = 2L;
        AuthUser authUSer = new AuthUser(product.getUser().getId(), product.getUser().getUsername(), "USER");

        //다른 상품(product2)에 속한 이미지 생성
        ProductImage wrongImage = ProductImage.of(product2, ImageType.THUMBNAIL, "worngImage.jpg", "hash");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findById(imageId)).thenReturn(Optional.of(wrongImage));

        //when&then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> productCommandService.deleteProductImage(productId, imageId, authUSer));

        assertEquals(ProductErrorCode.IMAGE_NOT_BELONGS_TO_PRODUCT, exception.getErrorCode());
    }
}