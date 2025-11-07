package org.example.lastcall.domain.product.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadToS3(MultipartFile file, String directory) {
        try {
            String fileName = directory + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            System.out.println("Uploading to S3 -> key: " + fileName + ", size: " + file.getSize());

            s3Client.putObject(builder -> builder
                            .bucket(bucketName)
                            .key(fileName)
                            .contentType(file.getContentType())
                            .build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            String url = s3Client.utilities()
                    .getUrl(builder -> builder.bucket(bucketName).key(fileName))
                    .toExternalForm();
            return url;
        } catch (IOException e) {
            throw new BusinessException(ProductErrorCode.RUNTIME_EXCEPTION);
        }
    }

    public void deleteFile(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private String extractKeyFromUrl(String imageUrl) {
        int index = imageUrl.indexOf(".com/");
        if (index != -1) {
            return imageUrl.substring(index + 5);//".com/"이후 문자열
        }
        throw new BusinessException(ProductErrorCode.INVALID_IMAGE_URL);
    }
}
