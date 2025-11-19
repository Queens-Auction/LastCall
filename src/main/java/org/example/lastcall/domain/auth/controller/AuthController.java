package org.example.lastcall.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.auth.exception.AuthErrorCode;
import org.example.lastcall.domain.auth.service.command.AuthCommandService;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth) API", description = "회원가입, 로그인, 로그아웃, 회원탈퇴 등 인증 관련 기능 제공")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthCommandService authCommandService;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "회원가입",
            description = "사용자는 회원가입 전 이메일 인증 API를 통해 인증을 완료하신 뒤 요청해야 합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> signup(@Valid @RequestBody SignupRequest request) {
        authCommandService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하며, Access/Refresh Token을 쿠키로 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody LoginRequest request) {
        if (request == null) {
            throw new BusinessException(AuthErrorCode.INVALID_EMPTY_EMAIL_OR_PASSWORD);
        }

        LoginResponse loginResponse = authCommandService.login(request);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(loginResponse.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(loginResponse.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success("로그인에 성공했습니다."));
    }

    @Operation(
            summary = "로그아웃",
            description = "사용자의 Refresh Token을 무효화하고, 인증 관련 쿠키를 삭제합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@CookieValue(name = CookieUtil.REFRESH_COOKIE, required = false) String refreshToken) {
        authCommandService.logout(refreshToken);

        ResponseCookie deleteAccessCookie = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefreshCookie = cookieUtil.deleteCookieOfRefreshToken();

        HttpHeaders headers = new HttpHeaders();

        headers.add(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.success("정상적으로 로그아웃되었습니다."));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 사용자가 본인 계정을 탈퇴합니다. 탈퇴 후 인증 쿠키는 삭제됩니다."
    )
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody(required = false) WithdrawRequest withdrawRequest) {
        if (authUser == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHENTICATED);
        }

        if (withdrawRequest == null || withdrawRequest.password() == null || withdrawRequest.password().isBlank()) {
            throw new BusinessException(AuthErrorCode.MISSING_PASSWORD);
        }

        authCommandService.withdraw(authUser.userId(), withdrawRequest);

        ResponseCookie deleteAccess = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefresh = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteAccess.toString())
                .header("Set-Cookie", deleteRefresh.toString())
                .body(ApiResponse.success("정상적으로 회원 탈퇴 요청이 완료되었습니다."));
    }

    @Operation(
            summary = "refresh token 재발급",
            description = "유효한 Refresh Token을 이용해 새로운 Access Token을 재발급합니다."
    )
    @PostMapping("/tokens")
    public ResponseEntity<ApiResponse<Object>> reissueToken(
            @CookieValue(name = CookieUtil.REFRESH_COOKIE, required = false) String refreshToken
    ) {
        LoginResponse response = authCommandService.reissueAccessToken(refreshToken);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(response.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success("토큰이 재발급되었습니다."));
    }
}
