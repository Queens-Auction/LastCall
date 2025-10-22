package org.example.lastcall.domain.product;

import org.example.lastcall.domain.product.entity.Category;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // 필터 실행 아예 무시 -> 인증 에러를 여기서 무시하도록 함.
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
//        productRepository.deleteAll(); //테스트 전 항상 초기화
//        userRepository.deleteAll();

        //테스트용 유저 생성
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

        User savedUser = userRepository.saveAndFlush(user);

        //테스트용 상품 생성
        productRepository.saveAndFlush(Product.of(savedUser,
                "해외직구한 샤넬백 미개봉 상품",
                Category.BAG,
                "설명임. 삼십자 이상이라서 길게 써야함 삼십자 이상이었나 기억이 안나지만 아무튼 길게 써야함 삼십자 이정도면 됏겠지 제발 하 테스트코드 어렵다 "));
        productRepository.saveAndFlush(Product.of(savedUser,
                "흰둥이 밥그릇 삼만원 부터 시작하겠습니다.",
                Category.KITCHEN,
                "백퍼센트 만족 보장합니다. 저도 이거 엄청 갖고 싶었는데, 운좋게 선물이 또 하나 들어와서 싸게 팝니다. 음식 담아서 인스타 용으로도 아주 좋습니다."));
    }

    @Test
    @DisplayName("상품전체 조회 API - 성공")
    void readAllProduct_succeess() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품을 전체 조회했습니다."))
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].name").exists());
    }

    @Test
    @DisplayName("상품 단건 조회 API - 성공")
    void readProduct_succeess() throws Exception {
        //상품 하나 가져오기
        Product product = productRepository.findAll().get(0);
        Long productId = product.getId();

        mockMvc.perform(get("/api/v1/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("상품 단건 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value(product.getName()))
                .andExpect(jsonPath("$.data.userId").value(product.getUser().getId()))
                .andExpect(jsonPath("$.data.category").value(product.getCategory().toString()))
                .andExpect(jsonPath("$.data.description").value(product.getDescription()))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.modifiedAt").exists());
    }
}
