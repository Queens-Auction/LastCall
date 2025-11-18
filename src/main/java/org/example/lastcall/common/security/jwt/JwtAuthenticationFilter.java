package org.example.lastcall.common.security.jwt;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.example.lastcall.common.util.DateTimeUtil;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
        HttpServletRequest req,
        HttpServletResponse res,
        FilterChain chain) throws ServletException, IOException {
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

            String publicId = claims.getSubject();
            Number uidNum = claims.get("uid", Number.class);
            String roleName = claims.get("role", String.class);

            if (uidNum == null) {
                log.warn("JWT 클레임 누락(uid or role)");
                SecurityContextHolder.clearContext();
                unauthorized(res);

                return;
            }

            Long userId = uidNum.longValue();
            Role role = Role.valueOf(roleName);
            User user = userRepository.findById(userId).orElse(null);

            if (user == null || user.isDeleted()) {
                log.warn("사용자 없음: userId={}", userId);
                SecurityContextHolder.clearContext();
                unauthorized(res);

                return;
            }

            if (user.isDeleted()) {
                log.warn("삭제된 사용자 접근 차단: userId={}", userId);
                unauthorized(res);

                return;
            }

            if (user.getPasswordChangedAt() != null) {
                LocalDateTime tokenIat = DateTimeUtil.convertToLocalDateTime(claims.getIssuedAt());

                if (tokenIat != null && tokenIat.isBefore(user.getPasswordChangedAt())) {
                    log.warn("비밀번호 변경 이후 이전 토큰 거부: userId={}", userId);
                    SecurityContextHolder.clearContext();
                    chain.doFilter(req, res);

                    return;
                }
            }

            AuthUser authUser = new AuthUser(userId, publicId, roleName);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            var authentication = new UsernamePasswordAuthenticationToken(authUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT 만료: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT 서명 불일치: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.warn("JWT 인증 실패: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(req, res);
    }

    private String resolveToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");

        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (CookieUtil.ACCESS_COOKIE.equals(c.getName())) {
                    String v = c.getValue();

                    return (v == null || v.isBlank()) ? null : v;
                }
            }
        }

        return null;
    }

    private void unauthorized(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
