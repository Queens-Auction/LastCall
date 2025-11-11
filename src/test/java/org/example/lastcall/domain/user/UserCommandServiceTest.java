package org.example.lastcall.domain.user;

import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.repository.RefreshTokenRepository;
import org.example.lastcall.domain.user.dto.request.PasswordChangeRequest;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.example.lastcall.domain.user.service.command.UserCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.BDDMockito.*;
        import static org.example.lastcall.domain.auth.enums.RefreshTokenStatus.*;

class UserCommandServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserCommandService userCommandService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = User.createForSignUp(
                UUID.randomUUID(),
                "tester",
                "nick",
                "test@email.com",
                "encodedPw",
                "Seoul",
                "12345",
                "Suwon",
                "010-1234-5678",
                Role.USER
        );
    }

    // updateMyProfile() 테스트
    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMyProfile_success() {
        // given
        Long userId = 1L;
        UserUpdateRequest req = new UserUpdateRequest("newNick", "010-7777-8888", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("newNick")).willReturn(false);

        // when
        UserProfileResponse res = userCommandService.updateMyProfile(userId, req);

        // then
        assertThat(res.nickname()).isEqualTo("newNick");
        assertThat(testUser.getPhoneNumber()).isEqualTo("010-7777-8888");
        then(userRepository).should(times(1)).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 수정 시 예외 발생")
    void updateMyProfile_userNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(1L, mock(UserUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 사용자 수정 시 예외 발생")
    void updateMyProfile_deletedUser() {
        // given
        testUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(1L, mock(UserUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_ALREADY_DELETED.getMessage());
    }

    @Test
    @DisplayName("닉네임 중복 시 예외 발생")
    void updateMyProfile_duplicateNickname() {
        // given
        Long userId = 1L;
        UserUpdateRequest req = new UserUpdateRequest("dupNick", null, null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("dupNick")).willReturn(true);

        // when & then
        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    // changeMyPassword() 테스트
    @Test
    @DisplayName("비밀번호 변경 성공")
    void changeMyPassword_success() {
        // given
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("oldPw", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("oldPw", testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPw", testUser.getPassword())).willReturn(false);
        given(passwordEncoder.encode("newPw")).willReturn("encodedNewPw");

        // when
        userCommandService.changeMyPassword(userId, req);

        // then
        assertThat(testUser.getPassword()).isEqualTo("encodedNewPw");
        then(refreshTokenRepository).should(times(1))
                .revokeAllActiveByUserId(testUser.getId(), ACTIVE, REVOKED);
    }

    @Test
    @DisplayName("비밀번호가 기존과 동일할 경우 예외 발생")
    void changeMyPassword_samePassword() {
        // given
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("oldPw", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("oldPw", testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPw", testUser.getPassword())).willReturn(true); // 같은 비밀번호

        // when & then
        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.SAME_AS_OLD_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("기존 비밀번호가 일치하지 않으면 예외 발생")
    void changeMyPassword_invalidOldPassword() {
        // given
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("wrongOld", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrongOld", testUser.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("삭제된 사용자가 비밀번호 변경 시 예외 발생")
    void changeMyPassword_deletedUser() {
        // given
        testUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(1L, mock(PasswordChangeRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_ALREADY_DELETED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 비밀번호 변경 시 예외 발생")
    void changeMyPassword_userNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(1L, mock(PasswordChangeRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }
}
