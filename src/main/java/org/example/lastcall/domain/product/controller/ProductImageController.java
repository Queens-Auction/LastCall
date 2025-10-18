package org.example.lastcall.domain.product.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.sevice.ProductImageServiceApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
