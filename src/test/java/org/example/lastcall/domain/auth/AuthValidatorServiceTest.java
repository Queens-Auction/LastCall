package org.example.lastcall.domain.auth;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthValidatorServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthValidatorService authValidatorService;

    @Test
    @DisplayName("이메일 인증 상태가 VERIFIED이면 예외 발생하지 않음")
    void validateEmailVerifiedStatus_VERIFIED_상태면_예외발생하지_않는다() {
        assertDoesNotThrow(() ->
                authValidatorService.validateEmailVerifiedStatus(EmailVerificationStatus.VERIFIED));
    }

    @Test
    @DisplayName("이메일 인증 상태가 VERIFIED가 아니면 예외 발생")
    void validateEmailVerifiedStatus_VERIFIED_아니면_예외발생한다() {
        assertThrows(BusinessException.class, () ->
                authValidatorService.validateEmailVerifiedStatus(EmailVerificationStatus.SENT));
    }

    @Test
    @DisplayName("RefreshToken이 유효하면 정상 통과")
    void validateRefreshToken_유효하면_정상통과한다() {
        RefreshToken token = mock(RefreshToken.class);
        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(token.isRevoked()).thenReturn(false);

        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any()))
                .thenReturn(Optional.of(token));

        assertDoesNotThrow(() -> authValidatorService.validateRefreshToken("token"));
    }

    @Test
    @DisplayName("RefreshToken이 존재하지 않으면 INVALID_REFRESH_TOKEN 예외 발생")
    void validateRefreshToken_존재하지않으면_INVALID_REFRESH_TOKEN_예외발생한다() {
        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("not_exist_token"));
    }

    @Test
    @DisplayName("만료된 RefreshToken이면 예외 발생")
    void validateRefreshToken_만료되면_예외발생한다() {
        RefreshToken token = mock(RefreshToken.class);
        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any()))
                .thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("token"));
    }

    @Test
    @DisplayName("RefreshToken이 취소(revoked)되었으면 REVOKED_REFRESH_TOKEN 예외 발생")
    void validateRefreshToken_취소되었으면_REVOKED_REFRESH_TOKEN_예외발생한다() {
        RefreshToken token = mock(RefreshToken.class);
        when(token.getExpiredAt()).thenReturn(LocalDateTime.now().plusDays(1));
        when(token.isRevoked()).thenReturn(true);

        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any()))
                .thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () ->
                authValidatorService.validateRefreshToken("revoked_token"));
    }
}
