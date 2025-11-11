package org.example.lastcall.domain.point.service.command;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.lastcall.common.AbstractIntegrationTest;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.fixture.PointFixture;
import org.example.lastcall.fixture.TestAuctionService;
import org.example.lastcall.fixture.TestBidService;
import org.example.lastcall.fixture.TestPointService;
import org.example.lastcall.fixture.TestUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class PointCommandServiceTest extends AbstractIntegrationTest {

	@Autowired
	private PointCommandService pointCommandService;

	@Autowired
	private PointRepository pointRepository;

	@Autowired
	private TestUserService testUserService;

	@Autowired
	private TestAuctionService testAuctionService;

	@Autowired
	private TestBidService testBidService;

	@Autowired
	private TestPointService testPointService;

	@DisplayName("동일 사용자의 포인트 충전 요청이 동시에 들어와도 중복 반영되지 않는다")
	@Test
	void createPoint_동시에_포인트_충전_요청이_들어와도_중복_반영되지_않음() throws InterruptedException {
		String email = "test101@gmail.com";
		String nickname = "닉네임1";

		User user = testUserService.saveTestUser(email, nickname);
		AuthUser authUser = new AuthUser(user.getId(), user.getPublicId().toString(), user.getUserRole().name());

		int threadCount = 5;    // 동시에 실행할 스레드(요청) 수
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);    // 스레드 풀 5개를 미리 만들어둠
		CountDownLatch latch = new CountDownLatch(threadCount);        // 스레드가 끝날 때마다 latch(잠금 카운트)를 하나씩 줄임

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointCommandService.createPoint(authUser, PointFixture.pointCreateRequest());
				} catch (Exception ignored) {
					System.out.println(ignored);
				} finally {
					latch.countDown();            // 스레드가 끝날 때마다 latch 감소 (5 → 0)
				}
			});
		}

		latch.await();    // latch 카운트가 0이 될 때까지 대기

		Point result = pointRepository.findByUserId(user.getId()).orElseThrow();

		assertThat(result.getAvailablePoint()).isEqualTo(
			PointFixture.pointCreateRequest().getIncomePoint() * threadCount);
	}

	@DisplayName("동시에 여러 입찰 요청이 들어와도 예치 포인트는 중복 차감되지 않는다")
	@Test
	void updateDepositPoint_동시에_여러_입찰이_들어와도_예치_포인트는_중복_차감되지_않음() throws InterruptedException {
		String email = "test102@gmail.com";
		String nickname = "닉네임2";
		var userPoint = 100000L;
		var bidStep = 100L;

		User user = testUserService.saveTestUser(email, nickname);
		Auction auction = testAuctionService.create(user, bidStep);
		Bid bid = testBidService.create(auction, user);
		Point point = testPointService.create(auction.getId(), bid.getId(), user, userPoint);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointCommandService.updateDepositPoint(auction.getId(), bid.getId(), bid.getBidAmount(),
						user.getId());
				} catch (Exception ignored) {
					System.out.println(ignored);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		Point result = pointRepository.findByUserId(user.getId()).orElseThrow();

		assertThat(result.getAvailablePoint()).isEqualTo(
			point.getAvailablePoint() - (auction.getStartingBid() + auction.getBidStep()));
		assertThat(result.getDepositPoint()).isEqualTo(auction.getStartingBid() + auction.getBidStep());
	}

	@DisplayName("동시에 여러 스레드가 정산 시도를 해도 한 번만 처리된다")
	@Test
	void depositToSettlement_동시에_정산_시도를_해도_한_번만_처리됨() throws InterruptedException {
		String email = "test203@gmail.com";
		String nickname = "닉네임3";

		var userPoint = 100000L;
		var bidStep = 100L;

		User user = testUserService.saveTestUser(email, nickname);

		Auction auction = testAuctionService.create(user, bidStep);
		Bid bid = testBidService.create(auction, user);

		var depositPoint = auction.getStartingBid() + auction.getBidStep();
		testPointService.createFirstDeposit(auction.getId(), bid.getId(), user, userPoint, depositPoint);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointCommandService.depositToSettlement(user.getId(), auction.getId(), depositPoint);
				} catch (Exception ignored) {
					System.out.println(ignored);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		Point result = pointRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(result.getSettlementPoint()).isEqualTo(depositPoint);
		assertThat(result.getAvailablePoint()).isEqualTo(userPoint - depositPoint);
	}

	@DisplayName("동시에 여러 스레드가 환불 요청을 해도 중복 환불되지 않는다")
	@Test
	void depositToAvailablePoint_동시에_환불_요청을_해도_한_번만_처리됨() throws InterruptedException {
		String email = "test104@gmail.com";
		String nickname = "닉네임4";
		var userPoint = 100000L;
		var bidStep = 100L;
		var loserEmail = "testLoser@example.com";
		var loserNickname = "진 사람";
		var winnerEmail = "testWinner@example.com";
		var winnerNickname = "이긴 사람";

		User seller = testUserService.saveTestUser(email, nickname);

		User loser = testUserService.saveTestUser(loserEmail, loserNickname);
		User winner = testUserService.saveTestUser(winnerEmail, winnerNickname);

		Auction auction = testAuctionService.create(seller, bidStep);

		Bid loserBid = testBidService.create(auction, loser);
		Bid winnerBid = testBidService.createMaxBid(auction, winner);

		var loserDepositPoint = auction.getStartingBid() + auction.getBidStep();
		var winnerDepositPoint = auction.getStartingBid() + auction.getBidStep() + auction.getBidStep();

		Point loserPoint = testPointService.createFirstDeposit(auction.getId(), loserBid.getId(), loser, userPoint,
			loserDepositPoint);
		Point winnerPoint = testPointService.createFirstDeposit(auction.getId(), winnerBid.getId(), winner, userPoint,
			winnerDepositPoint);

		int threadCount = 5;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					pointCommandService.depositToAvailablePoint(loser.getId(), auction.getId(), 0L);
				} catch (Exception ignored) {
					System.out.println(ignored);
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		Point loserResult = pointRepository.findByUserId(loser.getId()).orElseThrow();
		Point winnerResult = pointRepository.findByUserId(winner.getId()).orElseThrow();

		assertThat(loserResult.getDepositPoint()).isEqualTo(0L);
		assertThat(loserResult.getAvailablePoint()).isEqualTo(loserPoint.getAvailablePoint() + loserDepositPoint);

		assertThat(winnerResult.getDepositPoint()).isEqualTo(userPoint - winnerPoint.getAvailablePoint());
		assertThat(winnerResult.getAvailablePoint()).isEqualTo(winnerPoint.getAvailablePoint());
	}
}
