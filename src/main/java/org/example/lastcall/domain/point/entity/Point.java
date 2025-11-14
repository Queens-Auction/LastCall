package org.example.lastcall.domain.point.entity;

import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long availablePoint = 0L;

    @Column(name = "deposit_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long depositPoint = 0L;

    @Column(name = "settlement_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long settlementPoint = 0L;

    private Point(User user, PointLogType type, Long incomePoint) {
        this.user = user;
        this.type = type;
		this.availablePoint = incomePoint;
    }

    public static Point of(User user, PointLogType type, Long incomePoint) {
        return new Point(user, type, incomePoint);
    }

    public void updateAvailablePoint(Long incomePoint) {
		this.availablePoint = this.availablePoint + incomePoint;
	}

	public void updateDepositPoint(Long amount) {
		this.availablePoint -= amount;
		this.depositPoint += amount;
	}

	public void depositToSettlement(Long amount) {
		if (this.depositPoint < amount) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_DEPOSIT_POINT);
		}

		this.depositPoint -= amount;
		this.settlementPoint += amount;
	}

	public void decreaseAvailablePoint(Long amount) {
		if (this.availablePoint < amount) {
			throw new BusinessException(PointErrorCode.INSUFFICIENT_POINT);
		}

		this.availablePoint -= amount;
	}

	public void increaseDepositPoint(Long amount) {
		this.depositPoint += amount;
	}

	public void moveDepositToAvailable(Long amount) {
		this.depositPoint -= amount;
		this.availablePoint += amount;
	}

	public boolean canMoveDepositToAvailable(Long amount) {
		return this.depositPoint >= amount;
	}
}