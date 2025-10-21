package org.example.lastcall.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.sevice.ProductCommandServiceApi;
import org.example.lastcall.domain.product.sevice.ProductImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductImageController {
    private final ProductImageService productImageService;
    private final ProductCommandServiceApi productCommandServiceApi;

    @PostMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> createProductImage(@PathVariable Long productId,
                                                                                      @RequestBody List<ProductImageCreateRequest> requests) {
        List<ProductImageResponse> response = productCommandServiceApi.addImagesToProduct(productId, requests); //조율 메서드 호출
        //List<ProductImageResponse> response = productImageService.createProductImages(productId, requests);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("상품 이미지를 등록했습니다.", response)
        );
    }

    //대표 이미지 전체 조회 -> 리팩토링 때 삭제 예정
//    @GetMapping("/image")
//    public ResponseEntity<ApiResponse<PageResponse<ProductImageReadAllResponse>>> readThumbnailImages(Pageable pageable) {
//        PageResponse<ProductImageReadAllResponse> pageResponse = productImageService.readAllThumbnailImage(pageable.getPageNumber(), pageable.getPageSize());
//        ApiResponse<PageResponse<ProductImageReadAllResponse>> apiResponse = ApiResponse.success("대표 이미지 전체 조회 성공했습니다.", pageResponse);
//
//        return ResponseEntity.ok(apiResponse);
//    }

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
