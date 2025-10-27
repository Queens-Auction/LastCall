package org.example.lastcall.common.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @NotBlank String baseUrl,
        @Valid @NotNull Security security
) {

    public record Security(@Valid @NotNull Cookie cookie) {
        public record Cookie(boolean secure) {
        }
    }
}

