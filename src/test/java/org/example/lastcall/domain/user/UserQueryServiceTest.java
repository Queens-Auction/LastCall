package org.example.lastcall.domain.user;

import jakarta.persistence.EntityManager;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.example.lastcall.domain.user.service.query.UserQueryService;
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

class UserQueryServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserQueryService userQueryService;

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
                "Gangnam",
                "010-1234-5678",
                Role.USER
        );
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyProfile_success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        UserProfileResponse response = userQueryService.getMyProfile(1L);

        // then
        assertThat(response.nickname()).isEqualTo("nick");
        assertThat(response.email()).isEqualTo("test@email.com");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        then(userRepository).should(times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외 발생")
    void getMyProfile_userNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userQueryService.getMyProfile(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("삭제된 회원 조회 시 예외 발생")
    void getMyProfile_deletedUser() {
        // given
        testUser.softDelete();
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> userQueryService.getMyProfile(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_ALREADY_DELETED.getMessage());
    }

    @Test
    @DisplayName("findById 성공")
    void findById_success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        User found = userQueryService.findById(1L);

        // then
        assertThat(found).isEqualTo(testUser);
    }

    @Test
    @DisplayName("findById 실패 - 유저 없음")
    void findById_notFound() {
        // given
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userQueryService.findById(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getReferenceById - EntityManager 프록시 반환")
    void getReferenceById_success() {
        // given
        given(entityManager.getReference(User.class, 1L)).willReturn(testUser);
        User ref = userQueryService.getReferenceById(1L);

        // when & then
        assertThat(ref).isEqualTo(testUser);
        then(entityManager).should(times(1)).getReference(User.class, 1L);
    }
}
