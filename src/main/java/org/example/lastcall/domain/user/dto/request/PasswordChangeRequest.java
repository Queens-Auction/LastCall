package org.example.lastcall.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 변경 요청 DTO")
public record PasswordChangeRequest(
        @Schema(description = "현재 비밀번호", example = "OldPassword123!")
        @NotBlank
        String oldPassword,

        @Schema(description = "새 비밀번호", example = "NewPassword456!")
        @NotBlank
        String newPassword) {
}
