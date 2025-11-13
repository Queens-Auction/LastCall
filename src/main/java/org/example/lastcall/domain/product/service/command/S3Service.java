package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadToS3(MultipartFile file, String directory) {
        try {
            String key = directory + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            System.out.println("Uploading to S3 -> key: " + key + ", size: " + file.getSize());

            s3Client.putObject(builder -> builder
                            .bucket(bucketName)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            return key;
        } catch (IOException e) {
            throw new BusinessException(ProductErrorCode.RUNTIME_EXCEPTION_FOR_IMAGE_UPLOAD);
        }
    }

    public void deleteFile(String imageKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imageKey)
                    .build();

            log.info("[INFO] Deleted file from S3: {}", imageKey);
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            log.warn("[WARN] Failed to delete file from S3: {}", imageKey, e);
        }
    }

    public String generateImageUrl(String imageKey) {
        return s3Client.utilities()
                .getUrl(builder -> builder.bucket(bucketName).key(imageKey))
                .toExternalForm();
    }
}
