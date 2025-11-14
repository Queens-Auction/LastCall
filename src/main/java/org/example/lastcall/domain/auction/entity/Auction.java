package org.example.lastcall.domain.auction.entity;

import java.time.LocalDateTime;

import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.product.entity.Product;
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
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "auctions", indexes = {
	@Index(name = "idx_auction_deleted", columnList = "deleted"),
	@Index(name = "idx_auction_created_at", columnList = "created_at"),
	@Index(name = "idx_auction_user_deleted_created", columnList = "user_id, deleted, created_at DESC"),
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

	@Column(name = "current_bid")
	private Long currentBid;

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

	private Long version = 0L;

	private Auction(User user, Product product, Long startingBid, Long bidStep, LocalDateTime startTime, LocalDateTime endTime) {
		this.user = user;
		this.product = product;
		this.startingBid = startingBid;
		this.bidStep = bidStep;
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = (LocalDateTime.now().isBefore(startTime)) ? AuctionStatus.SCHEDULED : AuctionStatus.ONGOING;
	}

	public static Auction of(User user, Product product, AuctionCreateRequest request) {
		return new Auction(
			user,
			product,
			request.getStartingBid(),
			request.getBidStep(),
			request.getStartTime(),
			request.getEndTime());
	}

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

	public void updateStatus(AuctionStatus status) {
		this.status = status;
	}

	@Transient
	public AuctionStatus getDynamicStatus() {
		if (this.status == AuctionStatus.DELETED) {
			return this.status;
		}

		LocalDateTime now = LocalDateTime.now();

		if (now.isBefore(startTime)) {
			return AuctionStatus.SCHEDULED;
		}

		if (now.isBefore(endTime)) {
			return AuctionStatus.ONGOING;
		}

		if (winnerId != null && currentBid != null && currentBid > 0) {
			return AuctionStatus.CLOSED;
		} else {
			return AuctionStatus.CLOSED_FAILED;
		}
	}

	public void update(AuctionUpdateRequest request) {
		this.startingBid = request.getStartingBid();
		this.bidStep = request.getBidStep();
		this.startTime = request.getStartTime();
		this.endTime = request.getEndTime();
		this.status = determineStatus();
	}

	public void markAsDeleted() {
		this.status = AuctionStatus.DELETED;
		this.softDelete();
	}

	public boolean canClose() {
		return this.status != AuctionStatus.CLOSED
			&& this.status != AuctionStatus.CLOSED_FAILED
			&& this.status != AuctionStatus.DELETED;
	}

	public void closeAsFailed() {
		this.status = AuctionStatus.CLOSED_FAILED;
	}

	public void assignWinner(Long winnerId, Long winningBid) {
		this.winnerId = winnerId;
		this.currentBid = winningBid;
		this.status = AuctionStatus.CLOSED;
	}

	public void updateCurrentBid(Long currentBid) {
		this.currentBid = currentBid;
	}

	public void incrementParticipantCount() {
		this.participantCount++;
	}

	public void increaseVersion() {
		this.version++;
	}
}
