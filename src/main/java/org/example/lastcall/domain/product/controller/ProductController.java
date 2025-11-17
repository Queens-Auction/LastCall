package org.example.lastcall.domain.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.dto.request.ProductCreateRequest;
import org.example.lastcall.domain.product.dto.request.ProductUpdateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.dto.response.ProductReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.service.command.ProductCommandService;
import org.example.lastcall.domain.product.service.query.ProductQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "상품(Product) API", description = "상품 등록, 조회, 수정, 삭제 및 이미지 관리 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductCommandService productService;
    private final ProductQueryService productQueryService;

    @Operation(
            summary = "상품 등록",
            description = "로그인한 사용자가 새로운 상품을 등록합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품을 등록했습니다.", productService.createProduct(authUser, request)));
    }

    @Operation(
            summary = "상품 이미지 등록",
            description = "등록된 상품에 이미지를 추가합니다."
    )
    @PostMapping(
            value = "/{productId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> createProductImages(
            @PathVariable Long productId,
            @RequestPart("image") List<MultipartFile> image,
            @AuthenticationPrincipal AuthUser authUser) {
        List<ProductImageResponse> response = productService.createProductImages(productId, image, authUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("상품 이미지를 추가등록했습니다.", response));
    }

    @Operation(
            summary = "대표 이미지 지정",
            description = "상품의 대표 이미지를 지정합니다."
    )
    @PatchMapping("/{productId}/images/{imageId}/thumbnail")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> updateThumbnailImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthUser authUser) {
        List<ProductImageResponse> response = productService.setThumbnailImage(productId, imageId, authUser);
        ApiResponse<List<ProductImageResponse>> apiResponse = ApiResponse.success("대표 이미지 변경에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "내 상품 목록 조회",
            description = "로그인한 사용자가 등록한 상품 전체를 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<ProductReadAllResponse>>> getAllMyProducts(
            @AuthenticationPrincipal AuthUser authUser,
            Pageable pageable) {
        PageResponse<ProductReadAllResponse> pageResponse = productQueryService.getAllMyProducts(authUser, pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductReadAllResponse>> apiResponse = ApiResponse.success("상품을 전체 조회했습니다.", pageResponse);

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "상품 수정",
            description = "상품 정보를 수정합니다. (이미지 제외)"
    )
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal AuthUser authUser) {
        ProductResponse response = productService.updateProduct(productId, request, authUser);
        ApiResponse<ProductResponse> apiResponse = ApiResponse.success("상품 정보 업데이트에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    @Operation(
            summary = "상품 삭제",
            description = "상품 및 관련 이미지를 삭제합니다."
    )
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthUser authUser) {
        productService.deleteProduct(productId, authUser);

        return ResponseEntity.ok(ApiResponse.success("상품이 삭제되었습니다."));
    }

    @Operation(
            summary = "상품 이미지 삭제",
            description = "상품에 등록된 이미지를 삭제합니다."
    )
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthUser authUser) {
        productService.deleteProductImage(productId, imageId, authUser);

        return ResponseEntity.ok(ApiResponse.success("이미지 삭제에 성공했습니다."));
    }
}
