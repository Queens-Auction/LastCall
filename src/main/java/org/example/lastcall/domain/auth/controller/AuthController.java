package org.example.lastcall.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.auth.service.AuthService;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> signup(@Valid @RequestBody SignupRequest request)
    {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> LoginRequest(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = authService.userLogin(request);

        ResponseCookie accessCookie = cookieUtil.createAccessTokenCookie(loginResponse.accessToken());
        ResponseCookie refreshCookie = cookieUtil.createRefreshTokenCookie(loginResponse.refreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", accessCookie.toString());
                    httpHeaders.add("Set-Cookie", refreshCookie.toString());
                })
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> userLogout(@CookieValue(name = CookieUtil.REFRESH_COOKIE) String refreshToken) {
        authService.userLogout(refreshToken);
        ResponseCookie deleteAccessCookie = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefreshCookie = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add("Set-Cookie", deleteAccessCookie.toString());
                    httpHeaders.add("Set-Cookie", deleteRefreshCookie.toString());
                })
                .build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(@Auth AuthUser authUser,
                                                      @Valid @RequestBody WithdrawRequest withdrawRequest)
    {
        log.info("withdraw endpoint called, authUser={}", authUser);
        authService.withdraw(authUser.userId(), withdrawRequest);

        // 쿠키 삭제
        ResponseCookie deleteAccess  = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie deleteRefresh = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.noContent()
                .header("Set-Cookie", deleteAccess.toString())
                .header("Set-Cookie", deleteRefresh.toString())
                .build();
    }
}
