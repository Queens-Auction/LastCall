package org.example.lastcall.domain.point.entity;

import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// of 메서드 변경 가능 시 주석 제거 - 재귀 문제 완전 해결 //
// @Builder
// @AllArgsConstructor
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "point_logs",
	indexes = {
		@Index(name = "idx_point_log_user_created", columnList = "user_id, created_at DESC"),
		// FK 인덱스 : DB 호환성 위해 명시적 추가
		// user_id 는 복합 인덱스 사용 중이고, bid_id 는 사용 빈도 낮음
		@Index(name = "idx_point_log_auction", columnList = "auction_id"),
		@Index(name = "idx_point_log_point", columnList = "point_id")
	})
public class PointLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_id", nullable = false)
	private Point point;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "bid_id", insertable = false, updatable = false)   // Bid가 항상 존재하지 않는 로그도 있을 수 있기 때문에 nullable 허용
	private Bid bid;

	// TODO 추가한 코드
	@Column(name = "bid_id")
	private Long bidId;

	// TODO 수정한 영역 / 조회시 사용한다.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
	private User user;

	// TODO 등록 수정시 사용한다.
	@Column(name = "user_id")
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", insertable = false, updatable = false) // TODO insertable, updatale 추가
	private Auction auction;

	// TODO 추가한 코드
	@Column(name = "auction_id")
	private Long auctionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false)
	private PointLogType type;

	@Column(name = "description", nullable = false, length = 80)
	private String description;

	@Column(name = "point_change", nullable = false)
	private Long pointChange = 0L;

	@Column(name = "available_point_after", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
	private Long availablePointAfter = 0L;

	@Column(name = "deposit_point_after", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
	private Long depositPointAfter = 0L;

	@Column(name = "settlement_point_after", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
	private Long settlementPointAfter = 0L;

	private PointLog(Point point, Long userId, PointLogType type, String description, Long change,
		Long auctionId, Long bidId) {
		this.point = point;
		this.userId = userId;
		this.type = type;
		this.description = description;
		this.pointChange = change;
		this.auctionId = auctionId;
		this.bidId = bidId;
	}

	public static PointLog create(Point point, Long userId, PointLogType type, String description, Long change) {
		PointLog log = new PointLog();
		log.point = point;
		log.userId = userId;
		log.type = type;
		log.description = description;
		log.pointChange = change;
		return log;
	}

	public static PointLog create(Point point, Long userId, PointLogType type, String description, Long change,
		Long auctionId) {
		PointLog log = create(point, userId, type, description, change);
		log.auctionId = auctionId;
		return log;
	}

	public static PointLog create(Point point, Long userId, PointLogType type, String description, Long change,
		Long auctionId, Long bidId) {
		return new PointLog(point, userId, type, description, change, auctionId, bidId);
	}

	// 재귀 문제 완전 해결 방법 - 도메인 수정 가능 시 주석 해제
    /*public static PointLog of(Long userId, Long auctionId, Long amount, PointLogType type, String description) {
        // builder() 내부에서 다시 of() 호출하지 않도록 수정
        return new PointLog(
                null,   // id = 자동생성되므로
                null,   // point = 추후 Service 통해 주입됨
                null,   // bid = 입찰 관련 로그 아닐 수 있기 때문에 생략 가능
                null,   // user = userId는 있지만, User 엔티티 자체는 아직 로드 x
                null,   // auction = auctionId는 있지만, Auction 엔티티는 Service 에서 findById로 주입 예정
                type,   // 포인트 종류(enum)
                // description = type 있으면 enum 이름으로 description 세팅/ null 이면 "POINT_LOG"라는 기본값으로 대체
                type != null ? type.name() : "POINT_LOG",
                amount,  // pointChange 필드에 들어감
                0L, 0L, 0L
                // -> availablePointAfter, depositPointAfter, settlementPointAfter : 엔티티 생성 시점에서 값 미정
                // 0L로 초기화 (기본값 = 0 세팅)
        );
    }*/
}
