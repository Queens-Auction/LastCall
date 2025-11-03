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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_logs")
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


    public static PointLog create(Point point, User user, PointLogType type, String description, Long change) {
        PointLog log = new PointLog();
        log.point = point;
        log.user = user;
        log.type = type;
        log.description = description;
        log.pointChange = change;
        return log;
    }

    public static PointLog create(Point point, User user, PointLogType type, String description, Long change, Auction auction) {
        PointLog log = new PointLog();
        log.point = point;
        log.user = user;
        log.type = type;
        log.description = description;
        log.pointChange = change;
        log.auction = auction;
        return log;
    }

    @Builder
    public static PointLog of(Long userId, Long auctionId, Long amount, PointLogType type) {
        return PointLog.builder()
                .userId(userId)
                .auctionId(auctionId)
                .amount(amount)
                .type(type)
                .build();
    }


}
