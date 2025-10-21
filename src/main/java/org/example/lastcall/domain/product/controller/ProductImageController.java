package org.example.lastcall.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageReadAllResponse;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.ImageType;
import org.example.lastcall.domain.product.sevice.ProductImageService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductImageController {
    private final ProductImageService productImageService;

    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> createProductImage(@PathVariable Long productId,
                                                                                      @RequestBody List<ProductImageCreateRequest> requests) {
        List<ProductImageResponse> response = productImageService.createProductImages(productId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품 이미지를 등록했습니다.", response)
        );
    }

    //이미지 전체 조회(상품아이디와 썸네일만 "/api/v1/products/image?imageType=Thumbnail")
    @GetMapping("/image")
    public ResponseEntity<ApiResponse<PageResponse<ProductImageReadAllResponse>>> readThumbnailImages(@RequestParam ImageType imageType,
                                                                                                      Pageable pageable) {
        PageResponse<ProductImageReadAllResponse> pageResponse = productImageService.readAllThumbnailImage(imageType, pageable.getPageNumber(), pageable.getPageSize());
        ApiResponse<PageResponse<ProductImageReadAllResponse>> apiResponse = ApiResponse.success("대표 이미지 전체 조회 성공했습니다.", pageResponse);

        return ResponseEntity.ok(apiResponse);
    }

    //상품별 이미지 전체 조회
    @GetMapping("/{productId}/image")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> readAllProductImage(@PathVariable Long productId) {
        List<ProductImageResponse> response = productImageService.readAllProductImage(productId);
        ApiResponse<List<ProductImageResponse>> apiResponse = ApiResponse.success("상품별 이미지 조회에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }

    //상품 대표 이미지 변경 업데이트
    @PatchMapping("/{productId}/image/{imageId}")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> updateThumbnailImage(@PathVariable Long productId,
                                                                                        @PathVariable Long imageId) {
        List<ProductImageResponse> response = productImageService.updateThumbnailImage(productId, imageId);
        ApiResponse<List<ProductImageResponse>> apiResponse = ApiResponse.success("대표 이미지 변경에 성공했습니다.", response);

        return ResponseEntity.ok(apiResponse);
    }
}
