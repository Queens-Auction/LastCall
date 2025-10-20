package org.example.lastcall.domain.product;

import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.sevice.ProductImageService;
import org.example.lastcall.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ProductImageServiceTest {
    @Mock
    ProductImageRepository productImageRepository;

    @InjectMocks
    ProductImageService productImageService;

    private Product createTestProduct(Long id) throws Exception {
        User user = User.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .password("encodeed-Password!1")
                .nickname("tester")
                .address("Seoul")
                .postcode("12345")
                .detailAddress("Apt 101")
                .phoneNumber("010-2345-1234")
                .build();

        Product product = Product.of(user,
                "벽돌 1000장",
                Category.HOME_DECOR,
                "벽돌 1000장입니다. 울타리 쌓는데에도 좋고 벽 데코에도 아주 좋습니다. 다용도로 유용하게 사용할 수 있습니다. 가격은 오십만원부터 시작합니다.");

        Field idField = Product.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(product, id);

        return product;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("대표 이미지 수정 성공")
    void updateThumbnailImage_success() throws Exception {
        //given
        Long productId = 1L;
        Long newThumbnailId = 20L;

        Product product = createTestProduct(productId);

        ProductImage oldThumbnail = ProductImage.of(product, ImageType.THUMBNAIL, "url-1");
        ProductImage newThumbnail = ProductImage.of(product, ImageType.DETAIL, "url-2");

        //id 강제 설정 (리플렉션)
        setId(oldThumbnail, 10L);
        setId(newThumbnail, newThumbnailId);

        given(productImageRepository.findByProductIdAndImageType(productId, ImageType.THUMBNAIL))
                .willReturn(Optional.of(oldThumbnail));
        given(productImageRepository.findById(newThumbnailId))
                .willReturn(Optional.of(newThumbnail));
        given(productImageRepository.findAllByProductId(productId))
                .willReturn(List.of(oldThumbnail, newThumbnail));

        //when
        List<ProductImageResponse> responses = productImageService.updateThumbnailImage(productId, newThumbnailId);

        //then
        assertThat(oldThumbnail.getImageType()).isEqualTo(ImageType.DETAIL);
        assertThat(newThumbnail.getImageType()).isEqualTo(ImageType.THUMBNAIL);
        assertThat(responses).hasSize(2);
    }
}
