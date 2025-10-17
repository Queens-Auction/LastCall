package org.example.lastcall.domain.pointlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.bid.entity.BidEntity;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLogType;
import org.example.lastcall.domain.user.entity.UserEntity;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "point_logs")
public class PointLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_id", nullable = false)
    private Point point;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private BidEntity bid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

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

    @Column(name = "related_auction_id")
    private Long relatedAuctionId;

}
