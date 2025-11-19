package org.example.lastcall.domain.auth.email.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "이메일 인증 코드 검증 응답 DTO")
public class VerifyEmailVerificationCodeResponse {

    @Schema(description = "인증 완료 시 발급되는 공개 식별자(UUID)",
            example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID verificationPublicId;

    public VerifyEmailVerificationCodeResponse(UUID verificationPublicId) {
        this.verificationPublicId = verificationPublicId;
    }

    public UUID getVerificationPublicId() {
        return verificationPublicId;
    }
}
