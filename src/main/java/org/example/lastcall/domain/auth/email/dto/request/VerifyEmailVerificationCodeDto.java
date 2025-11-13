package org.example.lastcall.domain.auth.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "이메일 인증 검증 요청 및 응답 DTO")
public class VerifyEmailVerificationCodeDto {
    public record Request(
            @Schema(description = "인증받을 이메일 주소", example = "user@example.com")
            @Email
            String email,

            @Schema(description = "이메일로 발송된 인증 코드", example = "482910")
            @NotBlank
            String verificationCode) {
    }

    public record Response(
            @Schema(description = "인증 완료 시 발급되는 공개 식별자(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID verificationPublicId) {
    }
}
