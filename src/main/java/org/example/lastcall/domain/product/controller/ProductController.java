package org.example.lastcall.domain.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.sevice.ProductServiceApi;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductServiceApi productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(Long userId,  // 임시 유저 아이디
                                                                      //@AuthenticationPrincipal AuthUser authUser, 유저 인증인가 후 주석 해제
                                                                      @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품을 등록했습니다.", productService.createProduct(userId, request))
        );
    }

    //상품 전체 조회(상품 아이디와 상품명만 조회)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductReadAllResponse>>> readAllProduct(Pageable pageable) {
        PageResponse<ProductReadAllResponse> pageResponse = productService.readAllProduct(pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductReadAllResponse>> apiResponse = ApiResponse.success("상품을 전체 조회했습니다.", pageResponse);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 단건 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> readProduct(@PathVariable Long productId) {
        ProductResponse response = productService.readProduct(productId);
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success("상품 단건 조회에 성공했습니다.", response);
        return ResponseEntity.ok(apiResponse);
    }
}
