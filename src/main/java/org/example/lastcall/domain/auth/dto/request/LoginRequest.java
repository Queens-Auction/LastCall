package org.example.lastcall.domain.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(
        @Schema(description = "사용자 이메일 주소", example = "user@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "사용자 비밀번호", example = "mypassword123")
        @NotBlank
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password) {
}
