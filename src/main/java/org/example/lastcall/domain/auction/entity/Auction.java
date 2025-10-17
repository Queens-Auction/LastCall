package org.example.lastcall.domain.auction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "auctions")
public class Auction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "starting_bid", nullable = false)
    private Long startingBid;

    @Column(name = "bid_step", nullable = false)
    private Long bidStep;

    @Column(name = "current_bid", nullable = false)
    private Long currentBid;

    // Enum 매핑 명시 : 가독성 + 안정성
    // -> 안 써주면 기본값이 EnumType.ORDINAL (숫자 저장)
    // -> 나중에 Enum 순서 바뀌면 DB 꼬일 수 있음
    @Enumerated(EnumType.STRING)
    @Column(name = "auction_status", nullable = false)
    private AuctionStatus status;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(name = "participant_count", nullable = false)
    private Integer participantCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 추후  UserEntity -> User로 변경 예정
}
