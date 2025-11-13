package org.example.lastcall.domain.bid.service;

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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BidCommandServiceIntegrationTest extends AbstractIntegrationTest {

    // 실제 Bean 모두 로딩하여 서비스 계층 통합 테스트
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

    /**
     * 테스트 실행 전 공통 준비 데이터 생성
     * - 판매자/입찰자 생성
     * - 입찰자에게 기본 포인트 지급
     * - 진행중(ONGOING) 경매 생성
     */
    @BeforeEach
    void setUp() {
        seller = testUserService.saveTestUser("seller@test.com", "판매자");
        bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        // 기본 포인트 10만 지급 (auctionId, bidId는 임시로 null 처리)
        testPointService.create(null, null, bidder, 100_000L);

        // 진행 중인 경매 생성
        auction = testAuctionService.createOngoingAuction(seller, Category.ACCESSORY, 1000L, 100L);
    }

    @DisplayName("정상 입찰 성공 - 현재가 + 입찰 단위로 새로운 입찰 생성")
    @Test
    void createBid_정상입찰_성공() {
        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        // 정상 입찰 실행
        BidResponse response = bidCommandService.createBid(auction.getId(), authUser);

        // then
        // 응답 객체 생성 검증
        assertThat(response).isNotNull();
        // 입찰(현재가 + 입찰 단위) 검증
        assertThat(response.getBidAmount())
                .isEqualTo(auction.getStartingBid() + auction.getBidStep());
        // 입찰이 DB에 딱 1건 저장되었는지 검증
        assertThat(bidRepository.count()).isEqualTo(1);

        // 경매 현재가도 정상 업데이트 되었는지 확인
        Auction updated = testAuctionService.findById(auction.getId());
        assertThat(updated.getCurrentBid()).isEqualTo(response.getBidAmount());
    }

    @DisplayName("판매자가 본인 경매에 입찰 시 예외 발생")
    @Test
    void createBid_판매자본인경매_입찰시_예외발생() {
        // 판매자를 AuthUser로 구성 -> 본인 경매 입찰 시도
        AuthUser authUser = new AuthUser(seller.getId(), seller.getPublicId().toString(), seller.getUserRole().name());

        // then : 예외 발생해야 함
        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BidErrorCode.SELLER_CANNOT_BID.getMessage());
    }

    @DisplayName("포인트 부족 시 예외 발생")
    @Test
    void createBid_포인트부족_입찰실패() {
        User poorUser = testUserService.saveTestUser("poor@test.com", "유저");

        // 가진 포인트 50원만 세팅
        testPointService.create(null, null, poorUser, 50L);

        AuthUser poorAuth = new AuthUser(poorUser.getId(), poorUser.getPublicId().toString(), poorUser.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), poorAuth))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("포인트가 부족");
    }

    @DisplayName("종료(CLOSED)된 경매에 입찰 시 예외 발생")
    @Test
    void createBid_종료된경매_CLOSED_입찰불가() {
        auction.updateStatus(AuctionStatus.CLOSED);
        testAuctionService.save(auction);

        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("진행 중인 경매일 경우에만 입찰이 가능합니다.");
    }

    @DisplayName("종료(CLOSED_FAILED)된 경매에 입찰 시 예외 발생")
    @Test
    void createBid_종료된경매_CLOSED_FAILED_입찰불가() {
        auction.updateStatus(AuctionStatus.CLOSED_FAILED);
        testAuctionService.save(auction);

        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        assertThatThrownBy(() -> bidCommandService.createBid(auction.getId(), authUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("진행 중인 경매일 경우에만 입찰이 가능합니다.");
    }

    @DisplayName("동시에 여러 번 입찰해도 모두 성공하며 순차적으로 입찰 금액이 증가한다")
    @Test
    void createBid_동시요청_모두성공_순차적으로입찰증가() throws InterruptedException {
        // 동일 유저가 5개의 스레드로 동시 입찰 요청
        AuthUser authUser = new AuthUser(bidder.getId(), bidder.getPublicId().toString(), bidder.getUserRole().name());

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // 모든 스레드 종료 대기용
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 5개의 스레드가 동시 입찰 시도
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
        // 모든 스레드가 끝날때까지 대기
        latch.await();

        // 스레드 종료
        executor.shutdown();

        //입찰 5건이 모두 DB에 저장됐어야 함
        long bidCount = bidRepository.count();
        assertThat(bidCount).isEqualTo(threadCount);

        // 경매 현재가가 정확히 5번 증가했는지 확인
        Auction updated = testAuctionService.findById(auction.getId());
        assertThat(updated.getCurrentBid()).isEqualTo(
                auction.getStartingBid() + auction.getBidStep() * threadCount
        );
    }
}