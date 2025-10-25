package org.example.lastcall.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 DTO")
public record LoginResponse(
        @Schema(description = "Access Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.xxxxx.yyyyy")
        String accessToken,

        @Schema(description = "Refresh Token (JWT)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.zzzzz.wwwww")
        String refreshToken) {
}
