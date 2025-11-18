package org.example.lastcall.domain.product.service.validator;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.exception.ProductErrorCode;
import org.example.lastcall.domain.product.repository.ProductImageRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductValidatorService {
    private final ProductImageRepository productImageRepository;

    public void checkOwnership(Product product, AuthUser authUser) {
        if (!product.getUser().getId().equals(authUser.userId())) {
            throw new BusinessException(ProductErrorCode.ACCESS_DENIED);
        }
    }

    public <T> void validateImageCount(List<T> images) {
        if (images.size() > 10) {
            throw new BusinessException(ProductErrorCode.TOO_MANY_IMAGES);
        }
    }
}
