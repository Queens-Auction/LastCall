package org.example.lastcall.domain.auction;

import org.example.lastcall.common.AbstractIntegrationTest;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.fixture.TestAuctionService;
import org.example.lastcall.fixture.TestBidService;
import org.example.lastcall.fixture.TestPointService;
import org.example.lastcall.fixture.TestUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

// 매 테스트 실행 전 Application + DB + Redis 초기화
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuctionCommandServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private AuctionCommandService auctionCommandService;

    @Autowired
    private TestUserService testUserService;

    @Autowired
    private TestAuctionService testAuctionService;

    @Autowired
    private TestBidService testBidService;

    @Autowired
    private TestPointService testPointService;

    @DisplayName("동시에 여러 요청이 와도 동일 상품 경매는 한 번만 등록됨")
    @Test
    void createAuction_동시_여러_요청이_와도_동일_상품_경매는_한_번만_등록된다() throws InterruptedException {
        User seller = testUserService.saveTestUser("seller1@test.com", "판매자1");

        var request = TestAuctionService.AuctionFixture.createRequest();
        var product = testAuctionService.create(seller).getProduct();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    auctionCommandService.createAuction(product.getId(), seller.getId(), request);
                } catch (Exception ignored) {
                    System.out.println(ignored);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long count = testAuctionService.count();
        assertThat(count).isEqualTo(1);
    }

    @DisplayName("동시 종료 요청 - 입찰이 없으면 CLOSED_FAILED로 한 번만 처리")
    @Test
    void closeFailedAuction_입찰이_없으면_CLOSED_FAILED로_한_번만_처리된다() throws InterruptedException {
        User seller = testUserService.saveTestUser("seller2@test.com", "판매자2");

        Auction auction = testAuctionService.createOngoingAuction(
                seller, Category.ACCESSORY, 1000L, 100L
        );

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    auctionCommandService.closeAuction(auction.getId());
                } catch (BusinessException ignored) {
                    System.out.println(ignored);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Auction closed = testAuctionService.findById(auction.getId());
        assertThat(closed.getStatus()).isEqualTo(AuctionStatus.CLOSED_FAILED);
    }

    @DisplayName("동시 종료 요청 - 입찰 존재 시 낙찰 처리는 한 번만 수행")
    @Test
    void closeAuction_입찰이_있으면_CLOSED로_한_번만_처리된다() throws InterruptedException {
        User seller = testUserService.saveTestUser("seller3@test.com", "판매자3");
        User bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        Auction auction = testAuctionService.createOngoingAuction(
                seller, Category.ACCESSORY, 1000L, 100L
        );

        Bid bid = testBidService.createMaxBid(auction, bidder);

        Point point = testPointService.createForBid(auction.getId(), bid.getId(), bidder, 5000L);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    auctionCommandService.closeAuction(auction.getId());
                } catch (BusinessException ignored) {
                    System.out.println(ignored);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Auction closed = testAuctionService.findById(auction.getId());
        assertThat(closed.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(closed.getWinnerId()).isEqualTo(bidder.getId());
    }
}
