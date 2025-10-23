package org.example.lastcall.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.util.DateTimeUtil;
import org.example.lastcall.common.util.GeneratorUtil;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.exception.AuthErrorCode;
import org.example.lastcall.domain.auth.jwt.JwtUtil;
import org.example.lastcall.domain.auth.model.RefreshTokenStatus;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.example.lastcall.domain.user.exception.UserErrorCode.USER_ALREADY_DELETED;
import static org.example.lastcall.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    private void validateEmailVerifiedStatus(final EmailVerificationStatus status) {
        if (!Objects.equals(status, EmailVerificationStatus.VERIFIED)) {
            throw new BusinessException(EmailErrorCode.NOT_VERIFIED);
        }
    }

    @Transactional
    public void signup(final SignupRequest request) {
        // 이메일 인증 요청 존재 여부
        EmailVerification emailVerification = emailVerificationRepository
                .findByPublicId(request.getVerificationPublicId())
                .orElseThrow(() -> new BusinessException(EmailErrorCode.NOT_REQUESTED));

        // 검증 상태 확인
        validateEmailVerifiedStatus(emailVerification.getStatus()); // VERIFIED 아니면 예외

        // 인증 기록 소비 처리
        emailVerification.updateStatus(EmailVerificationStatus.CONSUMED);

        User user = User.createForSignUp(
                GeneratorUtil.generatePublicId(),
                request.getUsername(),
                request.getNickname(),
                emailVerification.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getAddress(),
                request.getPostcode(),
                request.getDetailAddress(),
                request.getPhoneNumber(),
                Role.USER
        );
        userRepository.save(user);
    }

    @Transactional
    public LoginResponse userLogin(final LoginRequest request) {
        String email = request.email().trim();

        // 1) 이메일 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        // 2) 탈퇴 계정 차단
        if (user.isDeleted()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_DELETED);
        }

        // 3) 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);

        Date refreshTokenExpiredDate = jwtUtil.getExpiration(refreshToken);
        LocalDateTime refreshTokenExpiredAt = DateTimeUtil.convertToLocalDateTime(refreshTokenExpiredDate);

        RefreshToken newRefreshToken = RefreshToken.create(
                user.getId(),
                refreshToken,
                RefreshTokenStatus.ACTIVE,
                refreshTokenExpiredAt
        );

        // 기존 ACTIVE 토큰들을 REVOKED로 변경
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndStatus(user.getId(), RefreshTokenStatus.ACTIVE);
        activeTokens.forEach(RefreshToken::revoke);

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    private RefreshToken validateRefreshToken(String requestedRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndStatus(requestedRefreshToken, RefreshTokenStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        final LocalDateTime now = LocalDateTime.now();
        if (refreshToken.getExpiredAt().isBefore(now)) {
            throw new BusinessException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        if (refreshToken.isRevoked()) {
            throw new BusinessException(AuthErrorCode.REVOKED_REFRESH_TOKEN);
        }

        return refreshToken;
    }

    @Transactional
    public void userLogout(final String requestedRefreshToken) {
        // refresh token 유효성 검증 및 조회
        RefreshToken refreshToken = validateRefreshToken(requestedRefreshToken);

        // 해당 사용자의 모든 활성 refresh token 무효화 (REVOKED)
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndStatus(
                refreshToken.getUserId(),
                RefreshTokenStatus.ACTIVE
        );
        activeTokens.forEach(RefreshToken::revoke);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // 1) 사용자 조회(삭제 안 된 사용자만) + 비밀번호 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (user.isDeleted()) throw new BusinessException(USER_ALREADY_DELETED);
        user.validatePassword(passwordEncoder, request.password());

        // 2) soft delete
        int updated = userRepository.softDeleteById(userId);
        if (updated == 0) {
            throw new BusinessException(USER_ALREADY_DELETED);
        }
        user.softDelete();

        // 3) 해당 사용자의 모든 활성 RT 무효화
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED);
    }
}
