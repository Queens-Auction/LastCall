package org.example.lastcall.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank String oldPassword,
        @NotBlank String newPassword
) {}

