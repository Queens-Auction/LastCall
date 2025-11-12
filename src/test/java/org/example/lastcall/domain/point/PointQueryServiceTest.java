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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
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
        authUser = mock(AuthUser.class);
        given(authUser.userId()).willReturn(1L);

        var userConstructor = User.class.getDeclaredConstructor();
        userConstructor.setAccessible(true);
        user = userConstructor.newInstance();

        var pointConstructor = Point.class.getDeclaredConstructor();
        pointConstructor.setAccessible(true);
        point = pointConstructor.newInstance();

        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(point, "id", 10L);
        ReflectionTestUtils.setField(point, "availablePoint", 5000L);
        ReflectionTestUtils.setField(point, "depositPoint", 2000L);
        ReflectionTestUtils.setField(point, "settlementPoint", 1000L);
    }

    @Test
    @DisplayName("내 포인트 조회 성공")
    void getUserPoint_내포인트조회_성공한다() {
        given(authUser.userId()).willReturn(1L);
        given(userServiceApi.findById(1L)).willReturn(user);
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        PointResponse response = pointQueryService.getUserPoint(authUser);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getAvailablePoint()).isEqualTo(5000L);
        assertThat(response.getDepositPoint()).isEqualTo(2000L);
        assertThat(response.getSettlementPoint()).isEqualTo(1000L);
        verify(pointRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("내 포인트 조회 실패 - 포인트 기록 없음 예외 발생")
    void getUserPoint_포인트기록없으면_예외발생한다() {
        given(authUser.userId()).willReturn(1L);
        given(userServiceApi.findById(1L)).willReturn(user);
        given(pointRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointQueryService.getUserPoint(authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(PointErrorCode.POINT_RECORD_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("입찰 가능 - 포인트 충분")
    void validateSufficientPoints_포인트충분하면_입찰가능하다() {
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        assertThatCode(() -> pointQueryService.validateSufficientPoints(1L, 3000L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("입찰 불가 - 포인트 부족 예외 발생")
    void validateSufficientPoints_포인트부족하면_예외발생한다() {
        given(pointRepository.findByUserId(1L)).willReturn(Optional.of(point));

        assertThatThrownBy(() -> pointQueryService.validateSufficientPoints(1L, 10000L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(PointErrorCode.INSUFFICIENT_POINT.getMessage());
    }
}
