package org.example.lastcall.domain.auction.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "auctions",
        indexes = {
                @Index(name = "idx_auction_deleted", columnList = "deleted"),
                // 아래 두 건은 인텔리제이에서 조회 속도 테스트 시 데이터가 적어 효과가 미비함
                // -> nGrinder로 부하테스트에서 성능 확인 예정
                // -> 부하 테스트에서도 효과가 미비하면 삭제 예정
                @Index(name = "idx_auction_created_at", columnList = "created_at"),
                @Index(name = "idx_auction_user_deleted_created", columnList = "user_id, deleted, created_at DESC"),
                // FK 인덱스 : DB 호환성 위해 명시적 추가
                @Index(name = "idx_auction_user_id", columnList = "user_id"),
                @Index(name = "idx_auction_product_id", columnList = "product_id")
        })
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

    @Column(name = "current_bid") // 등록값에서는 널 일수 있으므로 null 허용
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
    private int participantCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 빌더
    /* 엔티티의 @Builder는 생성자 단위로 붙이는 게 안전함
         - 클래스 상단에 붙이면 자동생성되는 id 까지 포함되므로 @GeneratedValue 위반 발생
         - @GeneratedValue : JPA 가 id를 자동 생성 (개발자 수동 세팅 금지)
         - 개발자가 JPA 관리 영역 침범하므로 정책 위반이 됨
    */
    // 현재 of 메서드에서 간접적으로 사용중
    @Builder
    private Auction(User user,
                    Product product,
                    Long startingBid,
                    Long bidStep,
                    LocalDateTime startTime,
                    LocalDateTime endTime) {
        this.user = user;
        this.product = product;
        this.startingBid = startingBid;
        this.bidStep = bidStep;
        this.startTime = startTime;
        this.endTime = endTime;
        // null 이면 determineStatus 로 자동 계산, status 명시되면 그대로 사용(테스트, 스케줄러용 예외 케이스)
        this.status = (status != null) ? status : determineStatus();
    }

    // 정적 팩토리 메서드 (of)
    public static Auction of(User user, Product product, AuctionCreateRequest request) {
        return Auction.builder()
                .user(user)
                .product(product)
                .startingBid(request.getStartingBid())
                .bidStep(request.getBidStep())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    // 경매 상태 계산 (엔티티 내부에서 처리)
    private AuctionStatus determineStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (this.status != null && status.equals(AuctionStatus.DELETED)) {
            return AuctionStatus.DELETED;
        } else if (now.isBefore(this.getStartTime())) {
            return AuctionStatus.SCHEDULED;
        } else if (now.isAfter(this.getEndTime())) {
            return AuctionStatus.CLOSED;
        } else {
            return AuctionStatus.ONGOING;
        }
    }

    // 동적 상태 조회용(표시용)
    // -> 조회 시점 상태 계산 메서드
    @Transient
    public AuctionStatus getDynamicStatus() {
        if (this.status == AuctionStatus.DELETED) {
            return this.status;
        }
        this.status = determineStatus();
        return this.status;
    }

    // 내 경매 수정
    public void update(AuctionUpdateRequest request) {
        this.startingBid = request.getStartingBid();
        this.bidStep = request.getBidStep();
        this.startTime = request.getStartTime();
        this.endTime = request.getEndTime();
        this.status = determineStatus();
    }

    // 경매 삭제 시 status 반영
    public void markAsDeleted() {
        this.status = AuctionStatus.DELETED;
        this.softDelete();
    }

    // 경매 종료 여부 확인
    public boolean canClose() {
        return this.status != AuctionStatus.CLOSED
                && this.status != AuctionStatus.DELETED;
    }

    // 경매 종료 처리 (유찰)
    public void closeAsFailed() {
        this.status = AuctionStatus.CLOSED_FAILED;
    }

    // 경매 낙찰자 확정
    public void assignWinner(Long winnerId, Long winningBid) {
        this.winnerId = winnerId;
        this.currentBid = winningBid;
        this.status = AuctionStatus.CLOSED; // 낙찰 성공
    }
}
