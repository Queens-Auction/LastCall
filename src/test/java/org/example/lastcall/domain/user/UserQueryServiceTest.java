package org.example.lastcall.domain.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;
import java.util.UUID;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.exception.UserErrorCode;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.example.lastcall.domain.user.service.query.UserQueryQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UserQueryQueryService userQueryService;

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
                "Gangnam",
                "010-1234-5678",
                Role.USER);
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyProfile_내_정보를_정상적으로_조회한다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        UserProfileResponse response = userQueryService.getMyProfile(1L);

        assertThat(response.nickname()).isEqualTo("nick");
        assertThat(response.email()).isEqualTo("test@email.com");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");

        then(userRepository).should(times(1)).findById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 예외 발생")
    void getMyProfile_존재하지_않는_사용자가_내_정보_조회_요청_시_예외가_발생한다() {
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getMyProfile(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @DisplayName("삭제된 회원은 인증 단계에서 차단되어 서비스 로직에 접근할 수 없다")
    @Test
    void getMyProfile_삭제된_회원은_인증_단계에서_차단된다() {
        testUser.softDelete();
        given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));

        UserProfileResponse response = userQueryService.getMyProfile(1L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("findById 성공")
    void findById_해당_유저_ID_조회에_성공한다() {
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        User found = userQueryService.findById(1L);

        assertThat(found).isEqualTo(testUser);
    }

    @Test
    @DisplayName("findById 실패 - 유저 없음")
    void findById_유저가_존재하지_않을_시_예외가_발생한다() {
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.findById(2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(UserErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("getReferenceById - EntityManager 프록시 반환")
    void findReferenceById_호출_시_프록시_객체를_반환한다() {
        given(entityManager.getReference(User.class, 1L)).willReturn(testUser);
        User ref = userQueryService.findReferenceById(1L);

        assertThat(ref).isEqualTo(testUser);

        then(entityManager).should(times(1)).getReference(User.class, 1L);
    }
}
