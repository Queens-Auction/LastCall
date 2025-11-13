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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.lastcall.domain.auth.enums.RefreshTokenStatus.ACTIVE;
import static org.example.lastcall.domain.auth.enums.RefreshTokenStatus.REVOKED;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
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
        testUser = User.of(
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

    @Test
    @DisplayName("내 정보 수정 성공")
    void updateMyProfile_내정보를정상적으로수정한다() {
        Long userId = 1L;
        UserUpdateRequest req = new UserUpdateRequest("newNick", "010-7777-8888", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("newNick")).willReturn(false);

        UserProfileResponse res = userCommandService.updateMyProfile(userId, req);

        assertThat(res.nickname()).isEqualTo("newNick");
        assertThat(testUser.getPhoneNumber()).isEqualTo("010-7777-8888");
        then(userRepository).should(times(1)).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 수정 시 예외 발생")
    void updateMyProfile_존재하지않는사용자수정요청시_예외가발생한다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(1L, mock(UserUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 사용자 수정 시 예외 발생")
    void updateMyProfile_삭제된사용자가수정요청시_예외를발생시킨다() {
        testUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(1L, mock(UserUpdateRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_ALREADY_DELETED.getMessage());
    }

    @Test
    @DisplayName("닉네임 중복 시 예외 발생")
    void updateMyProfile_닉네임이중복되면_예외를발생시킨다() {
        Long userId = 1L;
        UserUpdateRequest req = new UserUpdateRequest("dupNick", null, null);
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(userRepository.existsByNickname("dupNick")).willReturn(true);

        assertThatThrownBy(() ->
                userCommandService.updateMyProfile(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.DUPLICATE_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changeMyPassword_비밀번호를정상적으로변경한다() {
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("oldPw", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("oldPw", testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPw", testUser.getPassword())).willReturn(false);
        given(passwordEncoder.encode("newPw")).willReturn("encodedNewPw");

        userCommandService.changeMyPassword(userId, req);

        assertThat(testUser.getPassword()).isEqualTo("encodedNewPw");
        then(refreshTokenRepository).should(times(1))
                .revokeAllActiveByUserId(testUser.getId(), ACTIVE, REVOKED);
    }

    @Test
    @DisplayName("비밀번호가 기존과 동일할 경우 예외 발생")
    void changeMyPassword_기존비밀번호와동일할때_예외가발생한다() {
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("oldPw", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("oldPw", testUser.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPw", testUser.getPassword())).willReturn(true); // 같은 비밀번호

        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.SAME_AS_OLD_PASSWORD.getMessage());
    }

    @Test
    @DisplayName("기존 비밀번호가 일치하지 않으면 예외 발생")
    void changeMyPassword_기존비밀번호불일치시_예외가발생한다() {
        Long userId = 1L;
        PasswordChangeRequest req = new PasswordChangeRequest("wrongOld", "newPw");

        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrongOld", testUser.getPassword())).willReturn(false);

        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");
    }

    @Test
    @DisplayName("삭제된 사용자가 비밀번호 변경 시 예외 발생")
    void changeMyPassword_삭제된사용자비밀번호변경시_예외가발생한다() {
        testUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(1L, mock(PasswordChangeRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_ALREADY_DELETED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 비밀번호 변경 시 예외 발생")
    void changeMyPassword_존재하지않는사용자비밀번호변경시_예외가발생한다() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                userCommandService.changeMyPassword(1L, mock(PasswordChangeRequest.class)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }
}
