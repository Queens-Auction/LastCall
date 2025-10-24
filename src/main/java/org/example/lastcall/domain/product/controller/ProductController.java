package org.example.lastcall.domain.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadOneResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.sevice.command.ProductCommandService;
import org.example.lastcall.domain.product.sevice.query.ProductQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductCommandService productService;
    private final ProductQueryService productQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Auth AuthUser authUser,
                                                                      @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품을 등록했습니다.", productService.createProduct(authUser, request))
        );
    }

    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> createProductImage(@PathVariable Long productId,
                                                                                      @RequestBody List<ProductImageCreateRequest> requests) {
        List<ProductImageResponse> response = productService.createProductImages(productId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품 이미지를 등록했습니다.", response)
        );
    }

    //나의 상품 전체 조회(상품 아이디와 상품명만 조회+ 대표 이미지 + 내 상품만 전체조회)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ProductReadAllResponse>>> readAllProduct(@Auth AuthUser authUser,
                                                                                            Pageable pageable) {
        PageResponse<ProductReadAllResponse> pageResponse = productQueryService.readAllProduct(authUser, pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductReadAllResponse>> apiResponse = ApiResponse.success("상품을 전체 조회했습니다.", pageResponse);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 단건 조회(상품 이미지 목록도 응답값에 포함)
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductReadOneResponse>> readProduct(@PathVariable Long productId) {
        ProductReadOneResponse response = productQueryService.readProduct(productId);
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

    //상품 수정 시 이미지 추가 기능
    @PostMapping("/{productId}/images/append")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> appendProductImages(@PathVariable Long productId,
                                                                                       @RequestBody List<ProductImageCreateRequest> requests) {
        List<ProductImageResponse> response = productService.appendProductImages(productId, requests);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("상품 이미지를 추가등록했습니다.", response));
    }

    //상품 대표 이미지 변경 업데이트
    @PatchMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> updateThumbnailImage(@PathVariable Long productId,
                                                                                        @PathVariable Long imageId) {
        List<ProductImageResponse> response = productService.updateThumbnailImage(productId, imageId);
        ApiResponse<List<ProductImageResponse>> apiResponse = ApiResponse.success("대표 이미지 변경에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 삭제
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);

        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
    }

    //이미지 삭제
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(@PathVariable Long productId,
                                                                @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);

        return ResponseEntity.ok(ApiResponse.success("이미지 삭제에 성공했습니다."));
    }
}
