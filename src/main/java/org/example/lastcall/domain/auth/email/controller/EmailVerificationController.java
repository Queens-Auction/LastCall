package org.example.lastcall.domain.auth.email.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auth.email.dto.request.EmailVerificationSendRequest;
import org.example.lastcall.domain.auth.email.dto.request.VerifyEmailVerificationCodeDto;
import org.example.lastcall.domain.auth.email.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이메일 인증(Email Verification) API", description = "회원가입 전 이메일 인증 관련 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = "회원가입 전 이메일로 인증 코드를 발송합니다."
    )
    @PutMapping("/email-verifications")
    public ResponseEntity<ApiResponse<Object>> sendEmailVerificationCode(
            @Valid @RequestBody EmailVerificationSendRequest.Request request) {
        emailVerificationService.sendEmailVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
    }

    @Operation(
            summary = "이메일 중복 확인",
            description = "입력된 이메일이 이미 가입되어 있는지 중복 여부를 검사합니다."
    )
    @GetMapping("/auth/email/{email}/availability")
    public ResponseEntity<ApiResponse<Object>> validateDuplicateEmail(@PathVariable String email) {
        emailVerificationService.validateDuplicateEmail(email);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 이메일입니다."));
    }

    @Operation(
            summary = "이메일 인증 코드 검증",
            description = "사용자가 입력한 인증 코드를 검증하고, 유효 시 인증 완료 상태로 변경합니다."
    )
    @PutMapping("/email-verifications/status")
    public ResponseEntity<ApiResponse<VerifyEmailVerificationCodeDto.Response>> verifyEmailVerificationCode(
            @Valid @RequestBody VerifyEmailVerificationCodeDto.Request request) {
        var result = emailVerificationService.verifyEmailVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", result));
    }
}
