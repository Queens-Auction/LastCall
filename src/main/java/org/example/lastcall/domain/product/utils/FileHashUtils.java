package org.example.lastcall.domain.product.utils;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHashUtils {
    private FileHashUtils() {
    }

    public static String generateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new BusinessException(ProductErrorCode.RUNTIME_EXCEPTION_FOR_FILE_HASH);
        }
    }
}
