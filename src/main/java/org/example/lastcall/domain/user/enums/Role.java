package org.example.lastcall.domain.user.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Schema(description = "사용자 권한 (ADMIN 또는 USER)")
@Getter
@RequiredArgsConstructor
public enum Role {
    @Schema(description = "관리자 권한")
    ADMIN("ROLE_ADMIN"),

    @Schema(description = "일반 사용자 권한")
    USER("ROLE_USER");

    private final String key;
}
