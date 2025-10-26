package org.example.lastcall.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.TokenReissueRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.auth.service.command.AuthCommandService;
import org.example.lastcall.domain.auth.service.validator.AuthValidatorService;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
            description = "사용자가 이메일, 비밀번호, 닉네임 등을 입력해 회원가입을 진행합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> signup(@Valid @RequestBody SignupRequest request) {
        authCommandService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하며, Access/Refresh Token을 쿠키로 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authCommandService.login(request);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(loginResponse.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(loginResponse.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", accessCookie.toString());
                    httpHeaders.add("Set-Cookie", refreshCookie.toString());
                })
                .build();
    }

    @Operation(
            summary = "로그아웃",
            description = "사용자의 Refresh Token을 무효화하고, 인증 관련 쿠키를 삭제합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = CookieUtil.REFRESH_COOKIE) String refreshToken) {
        authCommandService.logout(refreshToken);
        ResponseCookie deleteAccessCookie = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefreshCookie = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", deleteAccessCookie.toString());
                    httpHeaders.add("Set-Cookie", deleteRefreshCookie.toString());
                })
                .build();
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 사용자가 본인 계정을 탈퇴합니다. 탈퇴 후 인증 쿠키는 삭제됩니다."
    )
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@Auth AuthUser authUser,
                                                      @Valid @RequestBody WithdrawRequest withdrawRequest) {
        log.info("withdraw endpoint called, authUser={}", authUser);
        authCommandService.withdraw(authUser.userId(), withdrawRequest);

        // 쿠키 삭제
        ResponseCookie deleteAccess = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefresh = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.noContent()
                .header("Set-Cookie", deleteAccess.toString())
                .header("Set-Cookie", deleteRefresh.toString())
                .build();
    }

    /**
     * 리프레시 토큰을 이용해 AccessToken/RefreshToken 재발급
     */
    @PostMapping("/tokens")
    public ResponseEntity<LoginResponse> reissueToken(@RequestBody TokenReissueRequest request) {
        LoginResponse response = authCommandService.reissueAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}
