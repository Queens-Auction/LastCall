package org.example.lastcall.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank
        String password) {}
