package org.example.lastcall.domain.auth.utils;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.security.AuthProperties;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_REISSUE_PATH = "/api/v1/auth";
    private static final String ACCESS_TOKEN_USABLE_PATH = "/api/v1";
    private final AuthProperties authProperties;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE, token)
                .httpOnly(true)
                .secure(authProperties.security().cookie().secure())
                .sameSite(Cookie.SameSite.LAX.attributeValue())
                .path(ACCESS_TOKEN_USABLE_PATH)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(authProperties.security().cookie().secure())
                .sameSite(Cookie.SameSite.STRICT.attributeValue())
                .path(REFRESH_TOKEN_REISSUE_PATH)
                .build();
    }

    public ResponseCookie deleteCookieOfAccessToken() {
        return deleteCookie(ACCESS_COOKIE, Cookie.SameSite.LAX, ACCESS_TOKEN_USABLE_PATH);
    }

    public ResponseCookie deleteCookieOfRefreshToken() {
        return deleteCookie(REFRESH_COOKIE, Cookie.SameSite.STRICT, REFRESH_TOKEN_REISSUE_PATH);
    }

    private ResponseCookie deleteCookie(String name, Cookie.SameSite sameSite, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(authProperties.security().cookie().secure())
                .sameSite(sameSite.attributeValue())
                .path(path)
                .maxAge(0)
                .build();
    }
}
