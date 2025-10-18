package org.example.lastcall.domain.product;

import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class ProductImageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductImageRepository productImageRepository;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        productImageRepository.deleteAll();
        productRepository.deleteAll();

        //테스트용 유저 생성
        User user = User.builder()
                .username("tester")
                .email("test@exampl.com")
                .password("password1!")
                .nickname("테스터")
                .address("서울시 종로구")
                .postcode("12345")
                .detailAddress("101동 101호")
                .phoneNumber("010-0101-0101")
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        savedProduct = productRepository.saveAndFlush(Product.of(
                savedUser,
                "테스트상품",
                Category.BAG,
                "상품설명 테스트입니다. 테스트용 상품설명입니다. 상품설명입니다. 이곳은 상품설명란입니다."));

        //테스트용 이미지 저장
        productImageRepository.saveAndFlush(ProductImage.of(savedProduct, ImageType.THUMBNAIL, "doihapoihepoaiheopaighepoihapoieg"));
        productImageRepository.saveAndFlush(ProductImage.of(savedProduct, ImageType.DETAIL, "dapohaheoghaoeighaoieghaoeihaeopgihaepogihaepogi"));
    }

    @Test
    @DisplayName("대표 이미지 전체 조회 API - 성공")
    void readThumbnailImages_success() throws Exception {
        mockMvc.perform(get("/api/v1/products/image")
                        .param("imageType", "THUMBNAIL")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print()) // 여기에 JSON 출력됨
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("대표 이미지 전체 조회 성공했습니다."))
                .andExpect(jsonPath("$.data.content[0].productId").exists())
                .andExpect(jsonPath("$.data.content[0].imageType").value("THUMBNAIL"))
                .andExpect(jsonPath("$.data.content[0].imageUrl").exists());
    }

    @Test
    @DisplayName("상품별 이미지 전체 조회 API - 성공")
    void readAllProductImage_success() throws Exception {
        mockMvc.perform(get("/api/v1/products/{productId}/image", savedProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품별 이미지 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data[0].productId").value(savedProduct.getId()))
                .andExpect(jsonPath("$.data[0].imageType").exists())
                .andExpect(jsonPath("$.data[0].imageUrl").exists())
                .andExpect(jsonPath("$.data[1].productId").value(savedProduct.getId()))
                .andExpect(jsonPath("$.data[1].imageType").exists())
                .andExpect(jsonPath("$.data[1].imageUrl").exists());

    }

}
