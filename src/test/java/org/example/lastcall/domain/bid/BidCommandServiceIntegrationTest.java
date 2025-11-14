package org.example.lastcall.domain.bid;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.lastcall.common.AbstractIntegrationTest;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.bid.dto.response.BidResponse;
import org.example.lastcall.domain.bid.exception.BidErrorCode;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.bid.service.command.BidCommandService;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.fixture.TestAuctionService;
import org.example.lastcall.fixture.TestPointService;
import org.example.lastcall.fixture.TestUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = testUserService.saveTestUser("seller@test.com", "판매자");
        bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        testPointService.create(null, null, bidder, 100_000L);

        auction = testAuctionService.createOngoingAuction(seller, Category.ACCESSORY, 1000L, 100L);
    }

    @DisplayName("정상 입찰 성공 - 현재가 + 입찰 단위로 새로운 입찰 생성")
    @Test
    void createBid_정상_입찰에_성공한다() {
        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        BidResponse response = bidCommandService.createBid(auction.getId(), authUser);

        assertThat(response).isNotNull();
        assertThat(response.getBidAmount()).isEqualTo(auction.getStartingBid() + auction.getBidStep());
        assertThat(bidRepository.count()).isEqualTo(1);

        Auction updated = testAuctionService.findById(auction.getId());
        assertThat(updated.getCurrentBid()).isEqualTo(response.getBidAmount());
    }

    @DisplayName("판매자가 본인 경매에 입찰 시 예외 발생")
    @Test
    void createBid_판매자_본인_경매_입찰_시_예외가_발생한다() {
        AuthUser authUser = new AuthUser(seller.getId(), seller.getPublicId().toString(), seller.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BidErrorCode.SELLER_CANNOT_BID.getMessage());
    }

    @DisplayName("포인트 부족 시 예외 발생")
    @Test
    void createBid_포인트_부족_시_입찰에_실패한다() {
        User poorUser = testUserService.saveTestUser("poor@test.com", "유저");

        testPointService.create(null, null, poorUser, 50L);

        AuthUser poorAuth = new AuthUser(poorUser.getId(), poorUser.getPublicId().toString(), poorUser.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), poorAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("포인트가 부족");
    }

    @DisplayName("종료(CLOSED)된 경매에 입찰 시 예외 발생")
    @Test
    void createBid_종료된_경매에_CLOSED_입찰이_불가하다() {
        auction.updateStatus(AuctionStatus.CLOSED);
        testAuctionService.save(auction);

        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("진행 중인 경매일 경우에만 입찰이 가능합니다.");
    }

    @DisplayName("종료(CLOSED_FAILED)된 경매에 입찰 시 예외 발생")
    @Test
    void createBid_종료된경매_CLOSED_FAILED_입찰이_불가하다() {
        auction.updateStatus(AuctionStatus.CLOSED_FAILED);
        testAuctionService.save(auction);

        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("진행 중인 경매일 경우에만 입찰이 가능합니다.");
    }

    @DisplayName("동시에 여러 번 입찰해도 모두 성공하며 순차적으로 입찰 금액이 증가한다")
    @Test
    void createBid_동시_요청_시_모두_성공하며_순차적으로_입찰이_증가한다() throws InterruptedException {
        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        int threadCount = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

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

        long bidCount = bidRepository.count();

        assertThat(bidCount).isEqualTo(threadCount);

        Auction updated = testAuctionService.findById(auction.getId());
        assertThat(updated.getCurrentBid()).isEqualTo(
                auction.getStartingBid() + auction.getBidStep() * threadCount);
    }
}