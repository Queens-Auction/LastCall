package org.example.lastcall.domain.auth.email.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auth.email.dto.request.EmailVerificationSendRequest;
import org.example.lastcall.domain.auth.email.dto.request.VerifyEmailVerificationCodeDto;
import org.example.lastcall.domain.auth.email.service.EmailVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<Object>> sendEmailVerificationCode(
            @Valid @RequestBody EmailVerificationSendRequest.Request request) {
        emailVerificationService.sendEmailVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
    }

    @GetMapping("/auth/email/{email}/availability")
    public ResponseEntity<ApiResponse<Object>> validateDuplicateEmail(@PathVariable String email) {
        emailVerificationService.validateDuplicateEmail(email);
        return ResponseEntity.ok(ApiResponse.success("사용 가능한 이메일입니다."));
    }

    @PutMapping("/email-verifications/status")
    public ResponseEntity<ApiResponse<VerifyEmailVerificationCodeDto.Response>> verifyEmailVerificationCode(
            @Valid @RequestBody VerifyEmailVerificationCodeDto.Request request) {
        var result = emailVerificationService.verifyEmailVerificationCode(request);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다.", result));
    }

}
