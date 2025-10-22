package org.example.lastcall.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "points")
public class Point extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PointLogType type;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "available_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long availablePoint = 0L;

    @Column(name = "deposit_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long depositPoint = 0L;

    @Column(name = "settlement_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long settlementPoint = 0L;

    public Point(User user, Long availablePoint, Long depositPoint, Long settlementPoint) {
        this.user = user;
        this.availablePoint = availablePoint;
        this.depositPoint = depositPoint;
        this.settlementPoint = settlementPoint;
    }

    public static Point create(User user, Long incomePoint) {
        Point point = new Point();
        point.user = user;
        point.availablePoint = incomePoint;
        return point;
    }

    public void updateAvailablePoint(Long incomePoint) {

        this.availablePoint += incomePoint;
    }

    public void updateDepositPoint(Long amount) {
        this.availablePoint -= amount;
        this.depositPoint += amount;
    }

    public void refundDepositPoint(Long amount) {
        this.depositPoint -= amount;
        this.availablePoint += amount;
    }

    public void DepositToSettlement(Long amount) {

        if (this.depositPoint < amount) {
            throw new BusinessException(PointErrorCode.INSUFFICIENT_DEPOSIT_POINT);
        }

        this.depositPoint -= amount;
        this.settlementPoint += amount;
    }
}

/*
    @Column 애너테이션에는 default 속성이 따로 없다.

    1. DB 테이블 생성 시점에 DEFAULT = 0을 넣고 싶다면 columnDefinition을 직접 지정해야한다.
    하지만 columnDefinition은 DB Dialect에 따라 문법 차이가 날 수 있다.

    2. DB에 Default를 두지 않고, 애플리케이션에서 새 엔티티 생성 시 자동으로 0이 들어가도록 하려면 필드에서 초기화 해도 된다.
    이 방식은 JPA persist 시점에 명시적으로 0이 insert 되기 때문에 db의 default는 사용하지 않는다.

    3. 두 방식 같이 써도 됨!
    두 방식을 같이 사용하면 DB 레벨에서는 default 0이 있고, Java 객체 생성 시에도 초기값이 0으로 들어가게 된다.
    양쪽에서 일관성 있게 0으로 시작하기 때문에 안전하게 사용 가능하고 실무에서도 많이 사용한다.
 */