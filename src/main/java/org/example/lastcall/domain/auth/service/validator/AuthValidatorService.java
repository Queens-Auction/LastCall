package org.example.lastcall.domain.auth.service.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.exception.AuthErrorCode;
import org.example.lastcall.domain.auth.enums.RefreshTokenStatus;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthValidatorService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void validateEmailVerifiedStatus(final EmailVerificationStatus status) {
        if (!Objects.equals(status, EmailVerificationStatus.VERIFIED)) {
            throw new BusinessException(EmailErrorCode.NOT_VERIFIED);
        }
    }

    public RefreshToken validateRefreshToken(String requestedRefreshToken) {
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
}
