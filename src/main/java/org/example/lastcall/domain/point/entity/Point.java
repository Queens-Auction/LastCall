package org.example.lastcall.domain.point.entity;

import jakarta.persistence.*;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.exception.PointErrorCode;
import org.example.lastcall.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "points")
public class Point {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long availablePoint = 0L;

    @Column(name = "deposit_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long depositPoint = 0L;

    @Column(name = "settlement_point", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long settlementPoint = 0L;

	@CreatedDate
	@Column(name = "created_at", updatable = false, nullable = false)
	private LocalDateTime createdAt;

    private Point(User user, Long incomePoint) {
        this.user = user;
		this.availablePoint = incomePoint;
    }

    public static Point of(User user, Long incomePoint) {
        return new Point(user, incomePoint);
    }

    public void updateAvailablePoint(Long incomePoint) {
		this.availablePoint += incomePoint;
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