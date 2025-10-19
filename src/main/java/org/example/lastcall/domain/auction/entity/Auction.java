package org.example.lastcall.domain.auction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
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

    @Column(name = "current_bid"/*, nullable = false*/) // 등록값에서는 널 일수 있으므로
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
    private User user;

    // 빌더 생성자
    @Builder
    private Auction(Product product,
                    User user,
                    Long startingBid,
                    Long bidStep,
                    LocalDateTime startTime,
                    LocalDateTime endTime,
                    AuctionStatus status) {
        this.product = product;
        this.user = user;
        this.startingBid = startingBid;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // 정적 팩토리 메서드 (of)
    public static Auction of(Product product, AuctionCreateRequest request, AuctionStatus status) {
        return Auction.builder()
                .product(product)
                .user(product.getUser())
                .startingBid(request.getStartingBid())
                .bidStep(request.getBidStep())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(status)
                .build();
    }
}
