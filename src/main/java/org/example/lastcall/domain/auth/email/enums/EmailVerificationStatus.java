package org.example.lastcall.domain.auth.email.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증 상태 ENUM")
public enum EmailVerificationStatus {
    @Schema(description = "인증 코드가 발송된 상태")
    SENT,

    @Schema(description = "이메일 인증이 완료된 상태")
    VERIFIED,

    @Schema(description = "인증 코드가 이미 사용된 상태 (회원가입 완료 시 등)")
    CONSUMED,

    @Schema(description = "인증 코드가 만료된 상태 (유효시간 초과)")
    EXPIRED
}
