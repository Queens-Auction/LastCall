package org.example.lastcall.domain.auction.service.command;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.event.AuctionEvent;
import org.example.lastcall.domain.auction.service.event.AuctionEventPublisher;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.point.service.command.PointCommandService;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.service.UserServiceApi;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionCommandService implements AuctionCommandServiceApi {

    private final AuctionRepository auctionRepository;
    private final UserServiceApi userServiceApi;
    private final ProductQueryServiceApi productQueryService;
    private final BidQueryServiceApi bidQueryServiceApi;
    private final PointCommandService pointCommandServiceApi;
    private final RabbitTemplate rabbitTemplate;
    private final AuctionEventPublisher auctionEventPublisher;

    // 경매 등록 //
    public AuctionResponse createAuction(Long productId, Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        productQueryService.validateProductOwner(productId, userId);
        // 2. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(productId)) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }
        // 3. User 조회
        User user = userServiceApi.findById(userId);
        // 4. 상품 조회
        Product product = productQueryService.findById(productId);
        // 5. Auction 엔티티 생성/저장 -> 경매 상태는 내부 로직에서 자동 계산됨
        Auction auction = Auction.of(user, product, request);
        auctionRepository.save(auction);
        // [RabbitMq 관련]

        /** 6. 경매 종료까지 남은 시간 계산
         * - Duration.between(현재시간, 종료시간)
         * - toMillis() : 밀리초 단위로 변환하여 delay 설정에 사용
         */
        Long delay = Duration.between(
                auction.getCreatedAt(),   // createAt 써야함. (예약된 경매도 있으므로)
                auction.getEndTime()
        ).toMillis();

        /** 7. 메시지큐로 경매 종료 이벤트 메시지 생성
         * - 이 이벤트 객체는 메시지큐에 전송되어 종료시점이 되면,
         * - Consumer (AuctionEventListener)가 꺼내서 closeAuction() 호출
         * - 낙찰자/낙찰가/유찰자는 종료 시점에 계산되므로 지금은 null
         */
        AuctionEvent event = new AuctionEvent(
                auction.getId(),
                null,      // 낙찰자 x -> 종료 시 계산
                null,              // 낙찰가 x -> 종료 시 계산
                null               // 유찰자 목록 x -> 종료 시 계산
        );

        /**8. 메시지큐에 경매 종료 이벤트 발행
         * - AuctionEventPublisher 내부에서 RabbitTemplate.convertAndSend() 사용
         * - x-delayed-message Exchange 를 통해 delay 만큼 대기 후 큐로 전달
         * - RabbitMQ 가 지연시간(delayMillis) 이후 케시지를 큐로 push
         * - AuctionEventListener 가 이를 수신하여 closeAuction() 자동 실행
         */
        auctionEventPublisher.sendAuctionEndEvent(event, delay);

        return AuctionResponse.fromCreate(auction);
    }

    // 내 경매 수정 //
    public AuctionResponse updateAuction(Long userId, Long auctionId, AuctionUpdateRequest request) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION);
        }
        auction.update(request);
        return AuctionResponse.fromUpdate(auction);
    }

    // 내 경매 삭제 //
    public void deleteAuction(Long userId, Long auctionId) {
        Auction auction = auctionRepository.findActiveById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        if (!auction.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }
        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION);
        }
        auction.markAsDeleted();
    }

    // 경매 상태 변경 (closed)
    public void closeAuction(Long auctionId) {
        // 1. 경매 엔티티 조회
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));

        // 2. 종료 여부 검증
        if (!auction.canClose()) {
            throw new BusinessException(AuctionErrorCode.AUCTION_ALREADY_CLOSED);
        }

        // 4. 최고 입찰자 조회
        Bid topBid = bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction).orElse(null);

        if (topBid != null) {
            // 입찰 존재 시, 낙찰 처리
            Long winnerId = topBid.getUser().getId();
            Long bidAmount = topBid.getBidAmount();

            // 낙찰 처리
            auction.assignWinner(winnerId, bidAmount);

            // 포인트 처리 (예치 -> 가용)
            pointCommandServiceApi.depositToAvailablePoint(winnerId, auction.getId(), bidAmount);

            // 잘되는지 테스트용
            System.out.printf("경매 종료 - 낙찰자 id: %d, 낙찰가: %d원%n", winnerId, bidAmount);
        } else {
            auction.closeAsFailed();
        }
        auctionRepository.save(auction);
    }
}
