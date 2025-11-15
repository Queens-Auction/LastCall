package org.example.lastcall.domain.product;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.service.command.ProductImageService;
import org.example.lastcall.domain.product.service.command.S3Service;
import org.example.lastcall.domain.product.utils.FileHashUtils;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductImageServiceTest {
    @Mock
    private S3Service s3Service;

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductImageService productImageService;

    private Product product;
    private MultipartFile file;

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

        file = new MockMultipartFile("file", "image.jpg", "image/jpeg", "content".getBytes());
    }

    @Test
    @DisplayName("이미지 업로드 후 ProductImage 생성 - 썸네일")
    void uploadAndCreateProductImage_이미지_업로드_후_썸네일을_생성한다() {
        ProductImageCreateRequest request = new ProductImageCreateRequest(true);

        when(s3Service.uploadToS3(file, "products/1")).thenReturn("s3-key.jpg");

        ProductImage image = productImageService.uploadAndCreateProductImage(product, file, "hash", 1L, ImageType.THUMBNAIL);

        assertNotNull(image);
        assertEquals(ImageType.THUMBNAIL, image.getImageType());
        assertEquals("s3-key.jpg", image.getImageKey());
    }

    @Test
    @DisplayName("이미지 업로드 후 ProductImage 생성 - 상세")
    void uploadAndCreateProductImage_이미지_업로드_후_상세_이미지를_생성한다() {
        ProductImageCreateRequest request = new ProductImageCreateRequest(false);

        when(s3Service.uploadToS3(file, "products/1")).thenReturn("s3-key-detail.jpg");

        ProductImage image = productImageService.uploadAndCreateProductImage(product, file, "hash", 1L, ImageType.DETAIL);

        assertNotNull(image);
        assertEquals(ImageType.DETAIL, image.getImageType());
        assertEquals("s3-key-detail.jpg", image.getImageKey());
    }

    @Test
    @DisplayName("중복 없는 파일 해시 생성 성공")
    void validateAndGenerateHashes_중복없는_파일이면_해시를_성공적으로_생성한다() {
        List<MultipartFile> files = List.of(file);

        when(productImageRepository.findAllByProductIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        Map<MultipartFile, String> result = productImageService.validateAndGenerateHashes(files, 1L);

        assertEquals(1, result.size());
        assertEquals(FileHashUtils.generateFileHash(file), result.get(file));
    }

    @Test
    @DisplayName("요청 내부 중복 파일 시 예외 발생")
    void validateAndGenerateHashes_요청_내부에_중복_파일_존재_시_예외가_발생한다() {
        List<MultipartFile> files = List.of(file, file);

        when(productImageRepository.findAllByProductIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productImageService.validateAndGenerateHashes(files, 1L));

        assertEquals(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("DB 중복 파일 시 예외 발생")
    void validateAndGenerateHashes_DB에_중복_파일_존재_시_예외가_발생한다() {
        ProductImage existingImage = ProductImage.of(product, ImageType.DETAIL, "key.jpg", FileHashUtils.generateFileHash(file));

        when(productImageRepository.findAllByProductIdAndDeletedFalse(1L)).thenReturn(List.of(existingImage));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> productImageService.validateAndGenerateHashes(List.of(file), 1L));

        assertEquals(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT, exception.getErrorCode());
    }
}
