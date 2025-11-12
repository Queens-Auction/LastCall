package org.example.lastcall.domain.bid.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "bids", indexes = {
        @Index(name = "idx_auction_id", columnList = "auction_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_auction_and_user", columnList = "auction_id, user_id")
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bid_amount", nullable = false)
    private Long bidAmount;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Bid(Long bidAmount, Auction auction, User user) {
        this.bidAmount = bidAmount;
        this.auction = auction;
        this.user = user;
    }

    public static Bid of(Long bidAmount, Auction auction, User user) {
        return new Bid(bidAmount, auction, user);
    }
}
