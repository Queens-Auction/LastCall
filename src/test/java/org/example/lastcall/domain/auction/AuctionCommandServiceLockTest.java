package org.example.lastcall.domain.auction;

import org.example.lastcall.common.AbstractIntegrationTest;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.repository.BidRepository;
import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.fixture.TestUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

// RabbitMQ Listener 비활성화
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
public class AuctionCommandServiceLockTest extends AbstractIntegrationTest {
    @Autowired
    private AuctionCommandService auctionCommandService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestUserService testUserService;
    @Autowired
    private BidRepository bidRepository;
    @Autowired
    private PointRepository pointRepository;

    @DisplayName("동시에 여러 요청이 와도 동일 상품 경매는 한 번만 등록된다.")
    @Test
    void createAuction_동시에_요청시_중복등록_방지() throws InterruptedException {
        // given
        // 테스트용 판매자 유저 생성
        User seller = testUserService.saveTestUser("seller1@test.com", "판매자1");

        // 테스트용 상품 생성
        Product product = productRepository.save(
                Product.of(seller, "테스트상품1", Category.ACCESSORY, "테스트 설명1"));
        // DB에 즉시 반영(쓰기) 명령
        productRepository.flush();

        // 경매 등록 요청 DTO 생성
        // ReflectionTestUtils : private 필드 강제 세팅 도구
        // 즉, private 필드 setter 없이 강제 세팅 가능
        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 1000L);
        ReflectionTestUtils.setField(request, "bidStep", 100L);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().plusSeconds(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusDays(1));

        int threadCount = 5;                                                   // 동시 실행할 요청 수(스레드)
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);  // 스레드 풀 생성
        // CountDownLatch : 여러 스레드가 끝날 때까지 기다리는 도구
        CountDownLatch latch = new CountDownLatch(threadCount);                // 스레드 종료 대기용 래치

        // when
        // 여러 스레드가 동시에 동일 상품에 대해 경매 등록 요청
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 경매 등록 시도
                    auctionCommandService.createAuction(product.getId(), seller.getId(), request);
                } catch (BusinessException ignored) {
                    // 이미 등록된 경우 예외 발생 예상 (DUPLICATE_AUCTION)
                    System.out.println("예외 발생: " + ignored.getMessage());
                } finally {
                    // 스레드 종료 시 Latch 감소
                    latch.countDown();
                }
            });
        }
        // 모든 스레드 종료될 때까지 대기
        latch.await();

        // then
        // 실제로 경매가 한 번만 등록되었는지 검증
        long auctionCount = auctionRepository.count();
        assertThat(auctionCount).isEqualTo(1); // 한번만 등록 되어야 함
    }

    @DisplayName("동시 종료 요청 - 입찰이 없으면 유찰(CLOSED_FAILED) 상태로 한 번만 처리된다.")
    @Test
    void closeAuction_동시요청_입찰없음_유찰처리_중복종료방지() throws InterruptedException {
        //given
        // 테스트용 판매자 유저 생성
        User seller = testUserService.saveTestUser("seller2@test.com", "판매자2");

        // 테스트용 상품 생성
        Product product = productRepository.save(
                Product.of(seller, "테스트상품2", Category.ACCESSORY, "테스트 설명2"));
        // DB에 즉시 반영(쓰기) 명령
        productRepository.flush();

        // 경매 사전 등록
        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 1000L);
        ReflectionTestUtils.setField(request, "bidStep", 100L);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().plusSeconds(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusDays(1));

        Auction auction = Auction.of(seller, product, request);
        auctionRepository.saveAndFlush(auction);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    auctionCommandService.closeAuction(auction.getId());
                } catch (BusinessException ignored) {
                    System.out.println("예외 발생: " + ignored.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        //then
        Auction closedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(closedAuction.getStatus()).isEqualTo(AuctionStatus.CLOSED_FAILED);
    }

    @DisplayName("동시 종료 요청 - 입찰이 존재하면 낙찰 처리는 한 번만 수행되고 중복 종료는 발생하지 않는다")
    @Test
    void closeAuction_동시요청_입찰있음_낙찰처리_중복종료방지() throws InterruptedException {
        //given
        // 테스트용 판매자 유저 생성
        User seller = testUserService.saveTestUser("seller3@test.com", "판매자3");
        // 테스트용 입찰자 유저 생성
        User bidder = testUserService.saveTestUser("bidder@test.com", "입찰자");

        // 테스트용 상품 생성
        Product product = productRepository.save(
                Product.of(seller, "테스트상품3", Category.ACCESSORY, "테스트 설명3"));
        // DB에 즉시 반영(쓰기) 명령
        productRepository.flush();

        // 포인트 계좌 생성
        //pointRepository.save(Point.create(bidder, PointLogType.EARN, 100000L));
        //pointRepository.flush();
        Point point = new Point(bidder, 0L, 5000L, 0L); // available=0, deposit=5000
        ReflectionTestUtils.setField(point, "type", PointLogType.EARN);
        pointRepository.saveAndFlush(point);

        // 경매 사전 등록
        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 1000L);
        ReflectionTestUtils.setField(request, "bidStep", 100L);
        // 이미 시작된 경매
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().minusMinutes(5));
        // 종료 시점 지난 상태로 설정
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().minusSeconds(1));

        Auction auction = Auction.of(seller, product, request);
        auctionRepository.saveAndFlush(auction);

        // 입찰 추가
        Bid bid = bidRepository.save(new Bid(2000L, auction, bidder));

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    auctionCommandService.closeAuction(auction.getId());
                } catch (BusinessException ignored) {
                    System.out.println("예외 발생: " + ignored.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        //then
        Auction closedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(closedAuction.getStatus()).isEqualTo(AuctionStatus.CLOSED);
        assertThat(closedAuction.getWinnerId()).isEqualTo(bidder.getId());
    }
}
