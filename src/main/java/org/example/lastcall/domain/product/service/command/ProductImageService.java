package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.dto.request.ProductImageCreateRequest;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.entity.ProductImage;
import org.example.lastcall.domain.product.enums.ImageType;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.example.lastcall.domain.product.utils.FileHashUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final S3Service s3Service;
    private final ProductImageRepository productImageRepository;

    public ProductImage uploadAndCreateProductImage(Product product,
                                                    ProductImageCreateRequest request,
                                                    MultipartFile file,
                                                    String fileHash,
                                                    Long productId) {
        String imageKey = s3Service.uploadToS3(file, "products/" + productId);
        ImageType imageType = Boolean.TRUE.equals(request.getIsThumbnail()) ? ImageType.THUMBNAIL : ImageType.DETAIL;

        return ProductImage.of(product, imageType, imageKey, fileHash);
    }

    public Map<MultipartFile, String> validateAndGenerateHashes(List<MultipartFile> files, Long productId) {
        Set<String> existingHashes = productImageRepository.findAllByProductIdAndDeletedFalse(productId)
                .stream()
                .map(ProductImage::getFileHash)
                .collect(Collectors.toSet());

        Map<MultipartFile, String> fileToHash = new HashMap<>();
        Set<String> newHashes = new HashSet<>();

        for (MultipartFile file : files) {
            String hash = FileHashUtils.generateFileHash(file);

            if (!newHashes.add(hash)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_REQUEST);
            }
            if (existingHashes.contains(hash)) {
                throw new BusinessException(ProductErrorCode.DUPLICATE_IMAGE_URL_IN_PRODUCT);
            }

            fileToHash.put(file, hash);
        }

        return fileToHash;
    }
}
