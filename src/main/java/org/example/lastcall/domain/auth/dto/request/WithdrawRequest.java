package org.example.lastcall.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "회원 탈퇴 요청 DTO")
public record WithdrawRequest(
        @Schema(description = "본인 확인용 비밀번호", example = "Lastcall123!")
        @NotBlank
        String password) {
}
