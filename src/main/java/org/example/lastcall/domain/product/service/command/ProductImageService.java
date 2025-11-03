package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final S3Service s3Service;

    public ProductImage uploadAndCreateProductImage(Product product, ProductImageCreateRequest request, Long productId) {
        String imageUrl = s3Service.uploadToS3(request.getMultipartFile(), "products/" + productId);
        ImageType imageType = Boolean.TRUE.equals(request.getIsThumbnail()) ? ImageType.THUMBNAIL : ImageType.DETAIL;

        return ProductImage.of(product, imageType, imageUrl);
    }
}
