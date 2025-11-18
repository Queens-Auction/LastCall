package org.example.lastcall.domain.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyLong;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.security.jwt.JwtUtil;
import org.example.lastcall.domain.auth.dto.request.LoginRequest;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.dto.request.WithdrawRequest;
import org.example.lastcall.domain.auth.dto.response.LoginResponse;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.enums.RefreshTokenStatus;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.auth.service.command.AuthCommandService;
import org.example.lastcall.domain.auth.service.validator.AuthValidatorService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class AuthCommandServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthValidatorService authValidatorService;

    @InjectMocks
    private AuthCommandService authCommandService;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.of(
                UUID.randomUUID(),
                "username",
                "nickname",
                "test@example.com",
                "encoded",
                "수원시",
                "12345",
                "상세주소",
                "01012345678",
                Role.USER);
    }

    @Test
    @DisplayName("회원가입 성공 시 이메일 인증, 중복 검증, 저장 수행")
    void signup_유효한_정보로_회원가입에_성공한다() {
        SignupRequest request = new SignupRequest();

        ReflectionTestUtils.setField(request, "verificationPublicId", UUID.randomUUID());
        ReflectionTestUtils.setField(request, "username", "짱구");
        ReflectionTestUtils.setField(request, "nickname", "흰둥이");
        ReflectionTestUtils.setField(request, "password", "Lastcall123!");
        ReflectionTestUtils.setField(request, "address", "수원");
        ReflectionTestUtils.setField(request, "postcode", "11111");
        ReflectionTestUtils.setField(request, "detailAddress", "상세");
        ReflectionTestUtils.setField(request, "phoneNumber", "01012345678");
        ReflectionTestUtils.setField(request, "verificationPublicId", UUID.randomUUID());

        EmailVerification mockVerification = mock(EmailVerification.class);
        mockVerification.updateStatus(EmailVerificationStatus.VERIFIED);

        when(mockVerification.getEmail()).thenReturn("test@example.com");
        when(emailVerificationRepository.findByPublicId(any(UUID.class))).thenReturn(Optional.of(mockVerification));
        when(userRepository.existsByNickname(anyString())).thenReturn(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        authCommandService.signup(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerificationRepository, times(1)).findByPublicId(any(UUID.class));
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 생성 및 RefreshToken 저장")
    void login_로그인_성공_시_토큰_생성_및_리프레시_토큰을_저장한다() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.createAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.createRefreshToken(any())).thenReturn("refresh-token");
        when(jwtUtil.getExpiration(anyString())).thenReturn(new Date());

        LoginResponse response = authCommandService.login(request);

        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 이메일 존재하지 않음")
    void login_로그인_실패_시_이메일_존재하지_않는다() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest("user@test.com", "1234");

        assertThrows(BusinessException.class, () -> authCommandService.login(request));
    }

    @Test
    @DisplayName("로그아웃 성공 시 활성 토큰들 REVOKE 처리")
    void logout_로그아웃_성공_시_활성_토큰을_폐기한다() {
        RefreshToken activeToken = RefreshToken.create(1L, "token", RefreshTokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        when(authValidatorService.validateRefreshToken(anyString())).thenReturn(activeToken);
        when(refreshTokenRepository.findByUserIdAndStatus(anyLong(), any())).thenReturn(List.of(activeToken));

        authCommandService.logout("valid-token");

        verify(refreshTokenRepository, times(1)).findByUserIdAndStatus(anyLong(), any());
    }

    @Test
    @DisplayName("회원 탈퇴 성공 시 Soft Delete 및 토큰 REVOKE 수행")
    void withdraw_회원_탈퇴_성공_시_소프트삭제_및_토큰_폐기한다() {
        WithdrawRequest request = new WithdrawRequest("password");

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(userRepository.softDeleteById(anyLong())).thenReturn(1);

        ReflectionTestUtils.setField(mockUser, "id", 1L);

        authCommandService.withdraw(1L, request);

        verify(refreshTokenRepository, times(1)).revokeAllActiveByUserId(anyLong(), any(), any());
    }

    @Test
    @DisplayName("리프레시 토큰 재발급 성공 시 새 AT/RT 생성 및 기존 RT REVOKE")
    void reissueAccessToken_재발급_성공_시_새_토큰_발급_및_기존_리프레시_토큰을_폐기한다() {
        RefreshToken validToken = RefreshToken.create(1L, "old-rt", RefreshTokenStatus.ACTIVE, LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenAndStatus(anyString(), any())).thenReturn(Optional.of(validToken));
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(mockUser));
        when(jwtUtil.createAccessToken(any())).thenReturn("new-at");
        when(jwtUtil.createRefreshToken(any())).thenReturn("new-rt");
        when(jwtUtil.getExpiration(anyString())).thenReturn(new Date());

        LoginResponse response = authCommandService.reissueAccessToken("old-rt");

        assertEquals("new-at", response.accessToken());
        assertEquals("new-rt", response.refreshToken());

        verify(refreshTokenRepository).save(any());
    }
}
