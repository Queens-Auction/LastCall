package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.service.validator.ProductValidatorService;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductValidatorTest {
    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductValidatorService productValidatorService;

    @Test
    @DisplayName("상품 소유자 불일치 시 예외 발생")
    void checkOwnership_상품_소유자가_불일치_시_예외가_발생한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);

        Product product = mock(Product.class);
        when(product.getUser()).thenReturn(user);

        AuthUser authUser = new AuthUser(2L, "otherUser", "USER");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productValidatorService.checkOwnership(product, authUser));

        assertEquals(ProductErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("이미지 개수가 10개 초과 시 예외 발생")
    void validateImageCount_이미지_개수_10개_초과_시_예외가_발생한다() {
        List<String> images = new ArrayList<>();

        for (int i = 0; i < 11; i++) images.add("img" + i);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productValidatorService.validateImageCount(images));

        assertEquals(ProductErrorCode.TOO_MANY_IMAGES, exception.getErrorCode());
    }
}
