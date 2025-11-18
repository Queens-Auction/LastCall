package org.example.lastcall.domain.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.auth.service.validator.AuthValidatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthValidatorServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthValidatorService authValidatorService;

    @Test
    @DisplayName("이메일 인증 상태가 VERIFIED이면 예외 발생하지 않음")
    void validateEmailVerifiedStatus_VERIFIED_상태면_예외가_발생하지_않는다() {
        assertDoesNotThrow(() ->
                authValidatorService.validateEmailVerifiedStatus(EmailVerificationStatus.VERIFIED));
    }

    @Test
    @DisplayName("이메일 인증 상태가 VERIFIED가 아니면 예외 발생")
    void validateEmailVerifiedStatus_VERIFIED_상태가_아니면_예외가_발생한다() {
        assertThrows(BusinessException.class, () ->
                authValidatorService.validateEmailVerifiedStatus(EmailVerificationStatus.SENT));
    }

    @Test
    @DisplayName("RefreshToken이 유효하면 정상 통과")
    void validateRefreshToken_토큰이_유효하면_정상_통과한다() {
        RefreshToken token = mock(RefreshToken.class);

        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(token.isRevoked()).thenReturn(false);
        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any())).thenReturn(Optional.of(token));

        assertDoesNotThrow(() -> authValidatorService.validateRefreshToken("token"));
    }

    @Test
    @DisplayName("RefreshToken이 존재하지 않으면 INVALID_REFRESH_TOKEN 예외 발생")
    void validateRefreshToken_토큰이_존재하지_않으면_INVALID_REFRESH_TOKEN_예외가_발생한다() {
        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("not_exist_token"));
    }

    @Test
    @DisplayName("만료된 RefreshToken이면 예외 발생")
    void validateRefreshToken_토큰이_만료되면_예외가_발생한다() {
        RefreshToken token = mock(RefreshToken.class);

        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("token"));
    }

    @Test
    @DisplayName("RefreshToken이 취소(revoked)되었으면 REVOKED_REFRESH_TOKEN 예외 발생")
    void validateRefreshToken_토큰이_취소_되었으면_REVOKED_REFRESH_TOKEN_예외가_발생한다() {
        RefreshToken token = mock(RefreshToken.class);

        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(token.isRevoked()).thenReturn(true);
        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any())).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("revoked_token"));
    }
}
