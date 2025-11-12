package org.example.lastcall.domain.point;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.point.dto.response.PointResponse;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.point.service.query.PointQueryService;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class PointQueryServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private UserServiceApi userServiceApi;

    @InjectMocks
    private PointQueryService pointQueryService;

    private AuthUser authUser;
    private User user;
    private Point point;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        authUser = mock(AuthUser.class);
        given(authUser.userId()).willReturn(1L);

        // protected 기본 생성자 강제 호출
        var userConstructor = User.class.getDeclaredConstructor();
        userConstructor.setAccessible(true);
        user = userConstructor.newInstance();

        var pointConstructor = Point.class.getDeclaredConstructor();
        pointConstructor.setAccessible(true);
        point = pointConstructor.newInstance();

        // private 필드 주입 (JPA 엔티티는 setter가 없으므로 Reflection 사용)
        ReflectionTestUtils.setField(user, "id", 1L); // 중요! (null 방지)
        ReflectionTestUtils.setField(point, "id", 10L);
        ReflectionTestUtils.setField(point, "availablePoint", 5000L);
        ReflectionTestUtils.setField(point, "depositPoint", 2000L);
        ReflectionTestUtils.setField(point, "settlementPoint", 1000L);
    }

    // 1. 내 포인트 조회 성공
    @Test
    @DisplayName("내 포인트 조회 성공")
    void getUserPoint_success() {
        // given
        given(authUser.userId()).willReturn(1L);
        given(userServiceApi.findById(1L)).willReturn(user);
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        // when
        PointResponse response = pointQueryService.getUserPoint(authUser);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAvailablePoint()).isEqualTo(5000L);
        assertThat(response.getDepositPoint()).isEqualTo(2000L);
        assertThat(response.getSettlementPoint()).isEqualTo(1000L);
        verify(pointRepository).findByUserId(1L);
    }

    // 2. 포인트 기록 없음 예외
    @Test
    @DisplayName("내 포인트 조회 실패 - 포인트 기록 없음 예외 발생")
    void getUserPoint_notFound() {
        // given
        given(authUser.userId()).willReturn(1L);
        given(userServiceApi.findById(1L)).willReturn(user);
        given(pointRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointQueryService.getUserPoint(authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(PointErrorCode.POINT_RECORD_NOT_FOUND.getMessage());
    }

    // 3. 입찰 가능 - 포인트 충분
    @Test
    @DisplayName("입찰 가능 - 포인트 충분")
    void validateSufficientPoints_success() {
        // given
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        // when & then
        assertThatCode(() -> pointQueryService.validateSufficientPoints(1L, 3000L))
                .doesNotThrowAnyException();
    }

    // 4. 입찰 불가 - 포인트 부족 예외
    @Test
    @DisplayName("입찰 불가 - 포인트 부족 예외 발생")
    void validateSufficientPoints_insufficient() {
        // given
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        // when & then
        assertThatThrownBy(() -> pointQueryService.validateSufficientPoints(1L, 10000L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(PointErrorCode.INSUFFICIENT_POINT.getMessage());
    }
}
