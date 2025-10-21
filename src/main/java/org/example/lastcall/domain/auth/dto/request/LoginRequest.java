package org.example.lastcall.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password
) {}
