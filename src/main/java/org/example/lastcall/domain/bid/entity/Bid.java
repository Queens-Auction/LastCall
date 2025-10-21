package org.example.lastcall.domain.bid.entity;

import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "bids")
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // 외부에서 new Bid() 하는 것을 막기 위함
public class Bid {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;    // 입찰 ID

	@Column(name = "bid_amount", nullable = false)
	private Long bidAmount;    // 입찰가

	@Column(name = "result_status")        // nullable = true (디폴트)
	@Enumerated(EnumType.STRING)
	private ResultStatus resultStatus;    // 입찰 상태

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "auction_id", nullable = false)
	private Auction auction;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// 생성자
	public Bid(Long bidAmount, ResultStatus resultStatus, Auction auction, User user) {
        this.bidAmount = bidAmount;
        this.resultStatus = resultStatus;
        this.auction = auction;
        this.user = user;
    }
}
