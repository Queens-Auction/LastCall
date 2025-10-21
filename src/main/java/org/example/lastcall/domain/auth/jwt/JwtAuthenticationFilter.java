package org.example.lastcall.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.domain.auth.utils.CookieUtil; // ACCESS_COOKIE 상수 사용 시
import org.example.lastcall.domain.user.enums.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증되어 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(req, res);
            return;
        }

        String token = resolveToken(req);
        if (token == null || token.isBlank()) {
            chain.doFilter(req, res);
            return;
        }

        try {
            Claims claims = jwtUtil.validateAndGetClaims(token);

            UUID userPublicId = UUID.fromString(claims.getSubject());
            String roleName = claims.get("role", String.class);

            Role role = Role.valueOf(roleName);

            var authorities = List.of(new SimpleGrantedAuthority(role.getKey()));
            var authentication = new UsernamePasswordAuthenticationToken(userPublicId, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(req, res);
    }

    // Authorization 헤더(Bearer) 우선, 없으면 access_token 쿠키
    private String resolveToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                // CookieUtil.ACCESS_COOKIE == "access_token"
                if (CookieUtil.ACCESS_COOKIE.equals(c.getName())) {
                    String v = c.getValue();
                    return (v == null || v.isBlank()) ? null : v;
                }
            }
        }
        return null;
    }
}
