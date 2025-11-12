package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.service.command.S3Service;
import org.example.lastcall.domain.product.service.query.ProductQueryService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductQueryServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ProductQueryService productQueryService;

    private Product product;
    private Product product2;
    private ProductImage thumbnailImage;
    private AuthUser authUser;
    private List<Product> productList;

    @BeforeEach
    void setUp() {
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
        authUser = new AuthUser(1L, "test@example.com", "ROLE_USER");
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

        productList = List.of(product, product2);
        thumbnailImage = ProductImage.of(product, ImageType.THUMBNAIL, "key.jpg", "hash");
    }

    @Test
    @DisplayName("내상품 전체 조회 성공")
    void getAllMyProduct_success() {
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 10), 1);

        when(productRepository.findAllByUserIdAndDeletedFalse(eq(authUser.userId()), any(Pageable.class)))
                .thenReturn(page);
        when(productImageRepository.findAllThumbnailsByProductIds(anyList()))
                .thenReturn(List.of(thumbnailImage));
        when(s3Service.generateImageUrl(thumbnailImage.getImageKey()))
                .thenReturn("url");

        PageResponse<ProductReadAllResponse> response = productQueryService.getAllMyProduct(authUser, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals("제가 그린 기린 그림", response.getContent().get(0).getName());
        assertEquals("url", response.getContent().get(0).getThumbnailUrl());
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void getProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findAllByProductIdAndDeletedFalse(1L)).thenReturn(List.of(thumbnailImage));
        when(s3Service.generateImageUrl(thumbnailImage.getImageKey())).thenReturn("url");

        ProductReadOneResponse response = productQueryService.getProduct(1L);

        assertEquals("제가 그린 기린 그림", response.getName());
        assertEquals(1, response.getImages().size());
        assertEquals("url", response.getImages().get(0).getImageUrl());
    }

    @Test
    @DisplayName("상품 단건 조회 실패 - 상품 없음")
    void getProduct_notFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productQueryService.getProduct(1L));
        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("썸네일 조회 성공 ")
    void findThumbnailImage_success() {
        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(1L, ImageType.THUMBNAIL))
                .thenReturn(Optional.of(thumbnailImage));
        when(s3Service.generateImageUrl(thumbnailImage.getImageKey())).thenReturn("url");

        ProductImageResponse response = productQueryService.findThumbnailImage(1L);

        assertEquals("url", response.getImageUrl());
    }

    @Test
    @DisplayName("썸네일 조회 실패 - 존재하지 않음")
    void findThumbnailImage_notFound() {
        when(productImageRepository.findByProductIdAndImageTypeAndDeletedFalse(1L, ImageType.THUMBNAIL))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productQueryService.findThumbnailImage(1L));

        assertEquals(ProductErrorCode.THUMBNAIL_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 소유자 검증 실패 - 소유자 불일치")
    void validateProductOwner_unauthorized() {

        Product userProduct = Product.of(product.getUser(), "p", Category.HOME_DECOR, "desc");
        when(productRepository.findByIdWithUser(1L)).thenReturn(Optional.of(userProduct));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productQueryService.validateProductOwner(1L, 999L));

        assertEquals(ProductErrorCode.UNAUTHORIZED_PRODUCT_OWNER, exception.getErrorCode());
    }

    @Test
    @DisplayName("상품 소유자 검증 실패 - 상품 없음")
    void validateProductOwner_notFound() {
        when(productRepository.findByIdWithUser(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productQueryService.validateProductOwner(1L, 1L));

        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());
    }
}
