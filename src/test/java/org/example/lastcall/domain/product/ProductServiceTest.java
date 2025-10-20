package org.example.lastcall.domain.product;

import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.product.sevice.ProductService;
import org.example.lastcall.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private Long productId;
    private Product product;

    @BeforeEach
    void setUp() {
        productId = 1L;
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

        product = Product.of(
                user,
                "제가 그린 기린 그림",
                Category.ART_PAINTING,
                "제가 그린 기린 그림입니다. 저는 여섯살 때부터 신바람 영재 미술 교실을 다닌 바가 있으며 계속 취미 생활을 유지중입니다."
        );

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
}
