package org.example.lastcall.domain.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.sevice.ProductCommandService;
import org.example.lastcall.domain.product.sevice.ProductViewService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductCommandService productService;
    private final ProductViewService productViewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Auth AuthUser authUser,
                                                                      @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품을 등록했습니다.", productService.createProduct(authUser.userId(), request))
        );
    }

    //나의 상품 전체 조회(상품 아이디와 상품명만 조회+ 대표 이미지 + 내 상품만 전체조회)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ProductReadAllResponse>>> readAllProduct(@Auth AuthUser authUser,
                                                                                            Pageable pageable) {
        PageResponse<ProductReadAllResponse> pageResponse = productViewService.readAllProduct(authUser.userId(), pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductReadAllResponse>> apiResponse = ApiResponse.success("상품을 전체 조회했습니다.", pageResponse);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 단건 조회(상품 이미지 목록도 응답값에 포함)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductReadOneResponse>> readProduct(@PathVariable Long productId) {
        ProductReadOneResponse response = productViewService.readProduct(productId);
        ApiResponse<ProductReadOneResponse> apiResponse = ApiResponse.success("상품 단건 조회에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 업데이트
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable Long productId,
                                                                      @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(productId, request);
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success("상품 정보 업데이트에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);

        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
    }
}
