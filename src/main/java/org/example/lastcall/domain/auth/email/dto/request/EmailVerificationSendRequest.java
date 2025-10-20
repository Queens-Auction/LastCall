package org.example.lastcall.domain.auth.email.dto.request;

import jakarta.validation.constraints.Email;

public class EmailVerificationSendRequest {
    public record Request(@Email(message = "이메일 형식이 아닙니다.") String email) {}
}

