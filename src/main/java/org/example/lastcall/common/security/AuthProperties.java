package org.example.lastcall.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @NotBlank String baseUrl,
        @Valid @NotNull Security security) {
    public record Security(@Valid @NotNull Cookie cookie) {
        public record Cookie(boolean secure) {}
    }
}
