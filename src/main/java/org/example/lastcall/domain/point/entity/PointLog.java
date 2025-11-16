package org.example.lastcall.domain.point.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_logs", indexes = {
        @Index(name = "idx_point_log_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_point_log_auction", columnList = "auction_id"),
        @Index(name = "idx_point_log_point", columnList = "point_id")
})
public class PointLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", insertable = false, updatable = false)
    private Bid bid;

    @Column(name = "bid_id")
    private Long bidId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", insertable = false, updatable = false) // TODO insertable, updatale 추가
    private Auction auction;

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

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    private PointLog(Point point, Long userId, PointLogType type, String description, Long change, Long auctionId, Long bidId) {
        this.point = point;
        this.userId = userId;
        this.type = type;
        this.description = description;
        this.pointChange = change;
        this.auctionId = auctionId;
        this.bidId = bidId;
    }

    public static PointLog of(Point point, Long userId, PointLogType type, String description, Long change) {
        PointLog log = new PointLog();

        log.point = point;
        log.userId = userId;
        log.type = type;
        log.description = description;
        log.pointChange = change;

        applyAfterValues(log, point);
        return log;
    }

    public static PointLog of(Point point, Long userId, PointLogType type, String description, Long change,
                              Long auctionId) {
        PointLog log = of(point, userId, type, description, change);

        log.auctionId = auctionId;

        applyAfterValues(log, point);
        return log;
    }

// 서영님 확인 후 삭제 필요
//    public static PointLog of(Point point, Long userId, PointLogType type, String description, Long change, Long auctionId, Long bidId) {
//        return new PointLog(
//                point,
//                userId,
//                type,
//                description,
//                change,
//                auctionId,
//                bidId);
//    }

    public static PointLog of(Point point, Long userId, PointLogType type, String description, Long change,
                              Long auctionId, Long bidId) {
        PointLog log = new PointLog(
                point,
                userId,
                type,
                description,
                change,
                auctionId,
                bidId
        );
        applyAfterValues(log, point);
        return log;
    }

    private static void applyAfterValues(PointLog log, Point point) {
        log.availablePointAfter = point.getAvailablePoint();
        log.depositPointAfter = point.getDepositPoint();
        log.settlementPointAfter = point.getSettlementPoint();
    }
}
