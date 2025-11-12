package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.service.validator.ProductValidator;
import org.example.lastcall.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductValidatorTest {
    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductValidator productValidator;

    @Test
    @DisplayName("상품 소유자 불일치 시 예외 발생")
    void checkOwnership_unauthorized() {
        //given
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Product product = mock(Product.class);
        when(product.getUser()).thenReturn(user);

        AuthUser authUser = new AuthUser(2L, "otherUser", "USER");

        //when&then
        BusinessException exception = assertThrows(BusinessException.class,
                () -> productValidator.checkOwnership(product, authUser));

        assertEquals(ProductErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미지 개수가 10개 초과 시 예외 발생")
    void validateImageCount_tooManyImages() {
        List<String> images = new ArrayList<>();
        for (int i = 0; i < 11; i++) images.add("img" + i);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productValidator.validateImageCount(images));

        assertEquals(ProductErrorCode.TOO_MANY_IMAGES, exception.getErrorCode());
    }

    @Test
    @DisplayName("썸네일 중복 시 예외 발생 -create")
    void validateThumbnailConsistencyForCreate_duplicateThumbnail() {
        List<ProductImageCreateRequest> requests = List.of(
                new ProductImageCreateRequest(true),
                new ProductImageCreateRequest(true)
        );
        when(productImageRepository.existsByProductIdAndImageTypeAndDeletedFalse(anyLong(), any()))
                .thenReturn(true);
        assertThrows(BusinessException.class,
                () -> productValidator.validateThumbnailConsistencyForCreate(1L, requests));
    }

    @Test
    @DisplayName("썸네일 중복 시 예외 발 - append")
    void validateThumbnailConsistencyForAppend_duplicateThmbnail() {
        List<ProductImage> images = List.of(
                ProductImage.of(mock(Product.class), ImageType.THUMBNAIL, "1.jpg", "hash"),
                ProductImage.of(mock(Product.class), ImageType.THUMBNAIL, "2.jpg", "hash2")
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productValidator.validateThumbnailConsistencyForAppend(images));

        assertEquals(ProductErrorCode.MULTIPLE_THUMBNAILS_NOT_ALLOWED, exception.getErrorCode());
    }
}
