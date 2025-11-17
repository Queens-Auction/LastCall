package org.example.lastcall.domain.bid;

import org.example.lastcall.common.AbstractIntegrationTest;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.bid.service.command.BidCommandService;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.fixture.TestAuctionService;
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

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BidCommandServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private BidCommandService bidCommandService;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private TestUserService testUserService;

    @Autowired
    private TestPointService testPointService;

    @Autowired
    private TestAuctionService testAuctionService;

    @DisplayName("동시에 여러 번 입찰해도 모두 성공하며 순차적으로 금액 증가")
    @Test
    void createBid_동시에_입찰_등록_요청이_들어올_시_순차적으로_금액이_증가한다() throws InterruptedException {
        // given
        User seller = testUserService.saveTestUser("seller@test.com", "판매자");
        User bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        testPointService.create(null, null, bidder, 100_000L);

        Auction auction = testAuctionService.createOngoingAuction(
                seller, Category.ACCESSORY, 1000L, 100L
        );

        AuthUser authUser = new AuthUser(
                bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name()
        );

        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when (동시 입찰 요청)
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    bidCommandService.createBid(auction.getId(), authUser);
                } catch (Exception ignored) {
                    System.out.println("예외 발생: " + ignored.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then
        assertThat(bidRepository.count()).isEqualTo(threadCount);

        Auction updated = testAuctionService.findById(auction.getId());
        assertThat(updated.getCurrentBid()).isEqualTo(
                auction.getStartingBid() + auction.getBidStep() * threadCount);
    }
}