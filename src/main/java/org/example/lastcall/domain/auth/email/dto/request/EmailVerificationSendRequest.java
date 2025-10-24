package org.example.lastcall.domain.auth.email.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "이메일 인증 코드 발송 요청 DTO")
public class EmailVerificationSendRequest {
    public record Request(
            @Schema(description = "인증 코드를 보낼 이메일 주소", example = "user@example.com")
            @Email(message = "이메일 형식이 아닙니다.")
            String email
    ) {
    }
}

