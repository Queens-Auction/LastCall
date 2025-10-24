package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.service.AuctionServiceApi;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.sevice.command.ProductCommandService;
import org.example.lastcall.domain.product.sevice.query.ProductQueryService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    ProductRepository productRepository;

    @Mock
    ProductImageRepository productImageRepository;

    @InjectMocks
    ProductCommandService productService;

    @InjectMocks
    ProductQueryService productQueryService;

    @Mock
    private AuctionServiceApi auctionServiceApi;

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
    @DisplayName("상품 전체 조회 시 각 상품의 대표 이미지 URL을 포함하여 반환한다")
    void readAllProduct_shouldIncludeThumbnailUrls() {
        //given
        int page = 0;
        int size = 10;
        Long userId = 1L;

        AuthUser authUser = new AuthUser(userId, "test@example.com", "ROLE_USER");
        Page<Product> productPage = new PageImpl<>(List.of(product, product2));

        given(productRepository.findAllByUserId(userId, PageRequest.of(page, size)))
                .willReturn(productPage);

        ProductImage thumbnail1 = ProductImage.of(product, ImageType.THUMBNAIL, "thumb1.jpg");
        ProductImage thumbnail2 = ProductImage.of(product2, ImageType.THUMBNAIL, "thumb2.jpg");

        given(productImageRepository.findAllThumbnailsByProductIds(List.of(1L, 2L)))
                .willReturn(List.of(thumbnail1, thumbnail2));

        //when
        PageResponse<ProductReadAllResponse> response = productQueryService.readAllProduct(authUser, page, size);

        //then
        assertThat(response.getContent()).hasSize(2);

        ProductReadAllResponse response1 = response.getContent().get(0);
        ProductReadAllResponse response2 = response.getContent().get(1);

        //응답에 대표 이미지 URL이 포함되어 있는지 확인
        assertThat(response1.getThumbnailUrl()).isEqualTo("thumb1.jpg");
        assertThat(response2.getThumbnailUrl()).isEqualTo("thumb2.jpg");

        verify(productRepository, times(1)).findAllByUserId(userId, PageRequest.of(page, size));
        verify(productImageRepository, times(1)).findAllThumbnailsByProductIds(List.of(1L, 2L));
    }

    @Test
    @DisplayName("상품 단건 조회 성공")
    void readProduct_success() {
        //given
        List<ProductImage> images = List.of(
                ProductImage.of(product, ImageType.THUMBNAIL, "imageUrl1.jpg"),
                ProductImage.of(product, ImageType.DETAIL, "imageUrl2.jpg")
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productImageRepository.findAllByProductId(productId)).thenReturn(images);

        //when
        ProductReadOneResponse response = productQueryService.readProduct(productId);

        //then
        assertNotNull(response);
        assertEquals(product.getId(), response.getId());
        assertEquals(product.getUser().getId(), response.getUserId());
        assertEquals(product.getName(), response.getName());
        assertEquals(2, response.getImages().size());
        assertEquals("imageUrl1.jpg", response.getImages().get(0).getImageUrl());

        verify(productRepository, times(1)).findById(productId);
        verify(productImageRepository, times(1)).findAllByProductId(productId);
    }

    @Test
    @DisplayName("상품 단건 조회 - 상품 없음")
    void readProduct_throwsException_whenProductNotFound() {
        //given
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //when&then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> productQueryService.readProduct(productId));

        assertEquals(ProductErrorCode.PRODUCT_NOT_FOUND, exception.getErrorCode());

        verify(productRepository, times(1)).findById(productId);
        verifyNoInteractions(productImageRepository);
    }

    @Test
    @DisplayName("상품 수정 - 성공")
    void updateProduct_success() {
        //given
        ProductUpdateRequest request = new ProductUpdateRequest(
                "내가 그린 기린 그림",
                Category.ART_CRAFT,
                "제가 직접 그린 기린 그림 입니다. 저는 네 살 때부터 미술 신동 소리를 들어왔으며 이 그림은 제가 일주일동안 그린 그림입니다.");

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        //when
        ProductResponse response = productService.updateProduct(productId, request);

        //then
        assertThat(response.getName()).isEqualTo("내가 그린 기린 그림");
        assertThat(response.getCategory()).isEqualTo(Category.ART_CRAFT);
        assertThat(response.getDescription()).isEqualTo("제가 직접 그린 기린 그림 입니다. 저는 네 살 때부터 미술 신동 소리를 들어왔으며 이 그림은 제가 일주일동안 그린 그림입니다.");
    }

    @Test
    @DisplayName("상품 수정 - 이름만 수정")
    void updateProduct_onlyName_success() {
        // given
        ProductUpdateRequest request = new ProductUpdateRequest(
                "새로운 이름", null, null
        );

        given(productRepository.findById(productId)).willReturn(Optional.of(product));

        //when
        ProductResponse response = productService.updateProduct(productId, request);

        //then
        assertThat(response.getName()).isEqualTo("새로운 이름");
        assertThat(response.getCategory()).isEqualTo(Category.ART_PAINTING); // 기존값 유지
        assertThat(response.getDescription()).isEqualTo("제가 그린 기린 그림입니다. 저는 여섯살 때부터 신바람 영재 미술 교실을 다닌 바가 있으며 계속 취미 생활을 유지중입니다.");
    }

    @Test
    @DisplayName("상품이 존재하면 softDelete 및 이미지 softDelete 호출")
    void deleteProduct_success() {
        //given
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        doNothing().when(auctionServiceApi).validateAuctionScheduled(productId);

        //when
        productService.deleteProduct(productId);

        //then
        assertTrue(product.isDeleted(), "상품이 soft deleted 되어야 함");
        verify(productImageRepository, times(1)).softDeleteByProductId(productId);
    }

    @Test
    @DisplayName("상품이 존재하지 않으면 예외 발생")
    void deleteProduct_notFound() {
        //given
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //when&then
        assertThrows(BusinessException.class, () -> productService.deleteProduct(productId));
        verify(productImageRepository, never()).softDeleteByProductId(any());
    }

    @Test
    @DisplayName("경매 전 상태가 아니면 삭제 불가 예외 발생")
    void deleteProduct_auctionNotScheduled() {
        //given: 실제 deleteProduct 호출 시 validateAuctionScheduled가 예외를 던지도록 Mock 구성
        //when(productRepository.findById(productId)).thenReturn(Optional.of(product)); 호출되지 않으므로 Mock 설정 제거
        doThrow(
                new BusinessException(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION))
                .when(auctionServiceApi).validateAuctionScheduled(productId);

        //when&then: deleteProduct 호출 → 예외 발생 확인
        BusinessException exception = assertThrows(BusinessException.class, () -> productService.deleteProduct(productId));

        assertEquals(AuctionErrorCode.CANNOT_MODIFY_PRODUCT_DURING_AUCTION, exception.getErrorCode());
        verify(productRepository, never()).findById(any());
        verify(productImageRepository, never()).softDeleteByProductId(any());
        verify(auctionServiceApi, times(1)).validateAuctionScheduled(productId);
    }
}
