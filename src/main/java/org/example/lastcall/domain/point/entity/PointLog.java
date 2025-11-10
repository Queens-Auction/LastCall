package org.example.lastcall.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.user.entity.User;

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
    @JoinColumn(name = "bid_id", nullable = true)   // Bid가 항상 존재하지 않는 로그도 있을 수 있기 때문에 nullable 허용
    private Bid bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

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


    // 일반 포인트 로그 (충전, 환급 등)
    public static PointLog create(Point point, User user, PointLogType type, String description, Long change) {
        PointLog log = new PointLog();
        log.point = point;
        log.user = user;
        log.type = type;
        log.description = description;
        log.pointChange = change;

        // after 값 자동 반영
        log.availablePointAfter = point.getAvailablePoint();
        log.depositPointAfter = point.getDepositPoint();
        log.settlementPointAfter = point.getSettlementPoint();

        return log;
    }

    // 입찰/정산 관련 로그
    public static PointLog create(Point point, User user, PointLogType type, String description, Long change, Auction auction, Bid bid) {
        PointLog log = new PointLog();
        log.point = point;
        log.user = user;
        log.type = type;
        log.description = description;
        log.pointChange = change;
        log.auction = auction;
        log.bid = bid;

        // after 값 자동 반영
        log.availablePointAfter = point.getAvailablePoint();
        log.depositPointAfter = point.getDepositPoint();
        log.settlementPointAfter = point.getSettlementPoint();

        return log;
    }

    // 경매 관련 로그
    public static PointLog create(Point point, User user, PointLogType type, String description, Long chahge, Auction auction) {
        PointLog log = new PointLog();
        log.point = point;
        log.user = user;
        log.type = type;
        log.description = description;
        log.pointChange = chahge;
        log.auction = auction;

        // after 값 자동 반영
        log.availablePointAfter = point.getAvailablePoint();
        log.depositPointAfter = point.getDepositPoint();
        log.settlementPointAfter = point.getSettlementPoint();

        return log;
    }

    /**
     * 임시 버전 (Builder 재귀 방지)
     * - StackOverflowError 방지용
     * - userId, auctionId 등은 실제 사용하지 않음
     * - 나중에 도메인 변경 시 아래 완전 해결 버전으로 교체 예정
     */
    @Builder
    public static PointLog of(Point point, User user, Auction auction, Long amount, PointLogType type) {
        // 임시 조치 [시작]
        // 재귀를 일으키는 builder() 호출 제거
        // 직접 객체를 만들어 필드 세팅
        PointLog log = new PointLog();

        // Lombok @Builder는 유지하되, 여기서는 수동으로 값 설정
        log.point = point;
        log.user = user;
        log.auction = auction;
        log.type = (type != null) ? type : PointLogType.DEPOSIT_TO_AVAILABLE; // 에러 해결
        log.pointChange = amount;

        // description 등은 필요시 추가 세팅 가능
        log.description = log.type.getDescription();

        // 추가
        log.availablePointAfter = point.getAvailablePoint();
        log.depositPointAfter = point.getDepositPoint();

        return log;
        // 임시 조치 [끝]

        // 기존 코드
        /*return PointLog.builder()
                .userId(userId)
                .auctionId(auctionId)
                .amount(amount)
                .type(type)
                .build();*/
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
