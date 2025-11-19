package org.example.lastcall.domain.auth.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증 코드 검증 요청 DTO")
public class VerifyEmailVerificationCodeRequest {

    @Schema(description = "인증받을 이메일 주소", example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "이메일로 발송된 인증 코드", example = "482910")
    @NotBlank
    private String verificationCode;

    public String getEmail() {
        return email;
    }

    public String getVerificationCode() {
        return verificationCode;
    }
}
