package org.example.lastcall.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.lastcall.domain.product.entity.ImageType;

@Getter
public class ProductImageCreateRequest {
    @NotNull
    private ImageType imageType;
    @NotBlank
    private String imageUrl;
}
