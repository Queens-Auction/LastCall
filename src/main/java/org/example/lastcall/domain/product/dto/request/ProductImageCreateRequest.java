package org.example.lastcall.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductImageCreateRequest {
    @NotNull
    private Boolean isThumbnail;
    @NotBlank
    private String imageUrl;
}
