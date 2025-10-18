package org.example.lastcall.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.sevice.ProductImageServiceApi;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductImageController {
    private final ProductImageServiceApi productImageService;

    @PostMapping("/{productId}/image")
    public ResponseEntity<ApiResponse<ProductImageResponse>> createProductImage(@PathVariable Long productId,
                                                                                @RequestBody ProductImageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품 이미지를 등록했습니다.", productImageService.createProductImage(productId, request))
        );
    }

    //이미지 전체 조회(상품아이디와 썸네일만 "/api/v1/products/image?imageType=Thumbnail")
    @GetMapping("/image?imageType=Thumbnail")
    public ResponseEntity<ApiResponse<PageResponse<ProductImageReadAllResponse>>> readThumbnailImages(Pageable pageable) {
        PageResponse<ProductImageReadAllResponse> pageResponse = productImageService.readAllThumbnailImage(pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductImageReadAllResponse>> apiResponse = ApiResponse.success("대표 이미지 전체 조회 성공했습니다.", pageResponse);
        return ResponseEntity.ok(apiResponse);
    }

    //상품별 이미지 전체 조회
    @GetMapping("/{productId}/image")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> readAllProductImage(@PathVariable Long productId) {
        List<ProductImageResponse> response = productImageService.readAllProductImage(productId);
        ApiResponse<List<ProductImageResponse>> apiResponse = ApiResponse.success("", response);
        return ResponseEntity.ok(apiResponse);
    }
}
