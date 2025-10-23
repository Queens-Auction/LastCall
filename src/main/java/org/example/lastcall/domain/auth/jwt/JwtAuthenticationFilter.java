package org.example.lastcall.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.util.DateTimeUtil;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.auth.utils.CookieUtil; // ACCESS_COOKIE 상수 사용 시
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException
    {

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

            // sub = UUID
            String publicId = claims.getSubject();

            // uid = PK(Long)
            Number uidNum = claims.get("uid", Number.class);
            if (uidNum == null) {
                log.warn("JWT에 uid 클레임이 없습니다.");
                unauthorized(res);
                return;
            }
            Long userId = uidNum.longValue();

            String roleName = claims.get("role", String.class);
            Role role = Role.valueOf(roleName);

            // DB에서 사용자 조회 (삭제 여부/비번변경 시각 확인)
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("사용자 없음: userId={}", userId);
                unauthorized(res);
                return;
            }
            if (user.isDeleted()) {
                log.warn("삭제된 사용자 접근 차단: userId={}", userId);
                unauthorized(res);
                return;
            }

            // 비밀번호 변경 이후 발급된 토큰만 허용
            // 토큰 발급 시각(iat) < passwordChangedAt 이면 거부
            if (user.getPasswordChangedAt() != null) {
                LocalDateTime tokenIat = DateTimeUtil.convertToLocalDateTime(claims.getIssuedAt());
                if (tokenIat.isBefore(user.getPasswordChangedAt())) {
                    log.warn("토큰이 비밀번호 변경 이전에 발급됨. 토큰 거부. userId={}", userId);
                    unauthorized(res);
                    return;
                }
            }

            // principal을 AuthUser로 (Long PK 중심)
            AuthUser authUser = new AuthUser(userId, publicId, roleName);

            var authorities = List.of(new SimpleGrantedAuthority(role.getKey()));
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

    private void unauthorized(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
    }
}
