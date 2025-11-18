package org.example.lastcall.domain.bid;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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

    @DisplayName("동시에 여러 스레드가 입찰 요청을 해도 1명만 입찰에 성공함")
    @Test
    void createBid_동시_입찰_시_1명만_성공한다() throws InterruptedException {
        User seller = testUserService.saveTestUser("seller@test.com", "판매자");
        User bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        testPointService.create(null, null, bidder, 100000L);

        Auction auction = testAuctionService.createOngoingAuction(
                seller, Category.ACCESSORY, 1000L, 100L);

        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        Long expectedNextBidAmount = auction.getStartingBid() + auction.getBidStep();

        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    bidCommandService.createBid(auction.getId(), authUser, expectedNextBidAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);

        assertThat(bidRepository.count()).isEqualTo(1);

        Auction updatedAuction = testAuctionService.findById(auction.getId());
        assertThat(updatedAuction.getCurrentBid()).isEqualTo(expectedNextBidAmount);
    }
}