package org.example.lastcall.domain.auth.service.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.security.jwt.JwtUtil;
import org.example.lastcall.common.util.DateTimeUtil;
import org.example.lastcall.common.util.GeneratorUtil;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.enums.RefreshTokenStatus;
import org.example.lastcall.domain.auth.exception.AuthErrorCode;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.auth.service.validator.AuthValidatorService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.example.lastcall.domain.user.exception.UserErrorCode.USER_ALREADY_DELETED;
import static org.example.lastcall.domain.user.exception.UserErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCommandService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthValidatorService authValidatorService;

    @Transactional
    public void signup(final SignupRequest request) {
        if (request.getVerificationPublicId() == null) {
            throw new BusinessException(EmailErrorCode.INVALID_VERIFICATION_ID);
        }
        EmailVerification emailVerification = emailVerificationRepository
                .findByPublicId(request.getVerificationPublicId())
                .orElseThrow(() -> new BusinessException(EmailErrorCode.NOT_REQUESTED));

        authValidatorService.validateEmailVerifiedStatus(emailVerification.getStatus());

        emailVerification.updateStatus(EmailVerificationStatus.CONSUMED);

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }

        userRepository.findByEmail(emailVerification.getEmail())
                .ifPresent(existingUser -> {
                    if (existingUser.isDeleted()) {
                        throw new BusinessException(UserErrorCode.DELETED_ACCOUNT);
                    }
                    throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
                });

        User user = User.of(
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
    public LoginResponse login(final LoginRequest request) {
        if (request.email() == null || request.email().trim().isEmpty() ||
                request.password() == null || request.password().trim().isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_EMPTY_EMAIL_OR_PASSWORD);
        }

        String email = request.email().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));

        if (user.isDeleted()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_DELETED);
        }

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

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndStatus(user.getId(), RefreshTokenStatus.ACTIVE);
        activeTokens.forEach(RefreshToken::revoke);

        refreshTokenRepository.save(newRefreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public LoginResponse reissueAccessToken(final String requestedRefreshToken) {
        RefreshToken validRefreshToken = validateRefreshToken(requestedRefreshToken);

        User user = userRepository.findById(validRefreshToken.getUserId())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (user.isDeleted()) {
            throw new BusinessException(AuthErrorCode.ACCOUNT_DELETED);
        }

        String newAccessToken = jwtUtil.createAccessToken(user);
        String newRefreshToken = jwtUtil.createRefreshToken(user);

        Date refreshTokenExpiredDate = jwtUtil.getExpiration(newRefreshToken);
        LocalDateTime refreshTokenExpiredAt = DateTimeUtil.convertToLocalDateTime(refreshTokenExpiredDate);

        RefreshToken newRtEntity = RefreshToken.create(
                user.getId(),
                newRefreshToken,
                RefreshTokenStatus.ACTIVE,
                refreshTokenExpiredAt
        );

        refreshTokenRepository.revokeAllActiveByUserId(
                user.getId(),
                RefreshTokenStatus.ACTIVE,
                RefreshTokenStatus.REVOKED
        );

        refreshTokenRepository.save(newRtEntity);

        return new LoginResponse(newAccessToken, newRefreshToken);
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
    public void logout(final String requestedRefreshToken) {
        if (requestedRefreshToken == null) {
            throw new BusinessException(AuthErrorCode.UNAUTHENTICATED);
        }

        RefreshToken refreshToken = authValidatorService.validateRefreshToken(requestedRefreshToken);

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndStatus(
                refreshToken.getUserId(),
                RefreshTokenStatus.ACTIVE
        );

        if (activeTokens == null || activeTokens.isEmpty()) {
            throw new BusinessException(AuthErrorCode.UNAUTHORIZED_ACCESS);
        }

        activeTokens.forEach(RefreshToken::revoke);
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));
        if (user.isDeleted()) throw new BusinessException(USER_ALREADY_DELETED);
        user.validatePassword(passwordEncoder, request.password());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }

        int updated = userRepository.softDeleteById(userId);
        if (updated == 0) {
            throw new BusinessException(USER_ALREADY_DELETED);
        }
        user.softDelete();

        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), RefreshTokenStatus.ACTIVE, RefreshTokenStatus.REVOKED);
    }
}
