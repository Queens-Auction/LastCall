package org.example.lastcall.domain.auction;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.request.AuctionUpdateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.command.AuctionCommandService;
import org.example.lastcall.domain.auction.service.command.AuctionEventScheduler;
import org.example.lastcall.domain.bid.entity.Bid;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.point.service.command.PointCommandService;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class AuctionCommandServiceTest {
    @InjectMocks
    private AuctionCommandService auctionCommandService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ProductQueryServiceApi productQueryServiceApi;

    @Mock
    private BidQueryServiceApi bidQueryServiceApi;

    @Mock
    private PointCommandService pointCommandService;

    @Mock
    private AuctionEventScheduler auctionEventScheduler;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("createAuction - 성공적으로 경매가 생성된다")
    void createAuction_유효한_요청이면_경매가_성공적으로_생성된다() {
        Long productId = 1L;
        Long sellerId = 10L;

        User user = mock(User.class);
        when(user.getId()).thenReturn(sellerId);

        Product product = mock(Product.class);
        when(product.getUser()).thenReturn(user);

        when(productQueryServiceApi.validateProductOwner(productId, sellerId))
                .thenReturn(product);

        when(auctionRepository.existsActiveAuction(productId)).thenReturn(false);

        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 1000L);
        ReflectionTestUtils.setField(request, "bidStep", 100L);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusHours(2));

        Auction savedAuction = mock(Auction.class);
        when(savedAuction.getId()).thenReturn(100L);
        when(auctionRepository.save(any(Auction.class))).thenReturn(savedAuction);

        AuctionResponse response = auctionCommandService.createAuction(productId, sellerId, request);

        assertThat(response).isNotNull();
        verify(auctionRepository).save(any(Auction.class));
        verify(auctionEventScheduler).scheduleAuctionEvents(any(Auction.class));
    }

    @Test
    @DisplayName("createAuction - 동일 상품에 대해 중복 경매는 생성할 수 없다")
    void createAuction_동일_상품에_대한_중복_경매는_생성할_수_없다() {
        Long productId = 1L;
        Long sellerId = 10L;

        User user = mock(User.class);
        Product product = mock(Product.class);
        when(product.getUser()).thenReturn(user);

        when(productQueryServiceApi.validateProductOwner(productId, sellerId))
                .thenReturn(product);

        when(auctionRepository.existsActiveAuction(productId)).thenReturn(true);

        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 1000L);
        ReflectionTestUtils.setField(request, "bidStep", 100L);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() ->
                auctionCommandService.createAuction(productId, sellerId, request)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.DUPLICATE_AUCTION.getMessage());
    }

    @Test
    @DisplayName("updateAuction - 판매자가 경매 정보를 성공적으로 수정할 수 있다")
    void updateAuction_판매자가_유효한_요청으로_경매를_성공적으로_수정한다() {
        Long userId = 10L;
        Long auctionId = 1L;

        AuctionUpdateRequest req = mock(AuctionUpdateRequest.class);
        when(req.getStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
        when(req.getEndTime()).thenReturn(LocalDateTime.now().plusHours(2));

        Auction auction = mock(Auction.class);

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(auction.getUser()).thenReturn(mockUser);

        when(auction.getStatus()).thenReturn(AuctionStatus.SCHEDULED);

        Product mockProduct = mock(Product.class);
        when(mockProduct.getId()).thenReturn(100L);
        when(auction.getProduct()).thenReturn(mockProduct);

        when(auctionRepository.findActiveById(auctionId))
                .thenReturn(Optional.of(auction));

        AuctionResponse response = auctionCommandService.updateAuction(userId, auctionId, req);

        assertNotNull(response);
        verify(auction).update(req);
        verify(auction).increaseVersion();
        verify(auctionRepository).save(any(Auction.class));
        verify(auctionEventScheduler).rescheduleAuctionEvents(any());
    }

    @Test
    @DisplayName("updateAuction - 진행중 또는 종료된 경매는 수정할 수 없다")
    void updateAuction_경매가_진행중이거나_종료된_상태라면_수정할_수_없다() {
        Long userId = 10L;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);

        Auction auction = mock(Auction.class);
        when(auction.getUser()).thenReturn(owner);
        when(auction.getStatus()).thenReturn(AuctionStatus.ONGOING);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        AuctionUpdateRequest request = mock(AuctionUpdateRequest.class);
        when(request.getStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
        when(request.getEndTime()).thenReturn(LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() ->
                auctionCommandService.updateAuction(userId, 1L, request)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION.getMessage());
    }

    @Test
    @DisplayName("updateAuction - 판매자가 아니면 수정할 수 없다")
    void updateAuction_판매자가_아니면_경매를_수정할_수_없다() {
        Long sellerId = 10L;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);

        Auction auction = mock(Auction.class);
        when(auction.getUser()).thenReturn(owner);
        when(auction.getStatus()).thenReturn(AuctionStatus.SCHEDULED);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        AuctionUpdateRequest request = new AuctionUpdateRequest();
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusHours(2));

        assertThatThrownBy(() ->
                auctionCommandService.updateAuction(sellerId, 1L, request)
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("deleteAuction - 판매자가 경매를 성공적으로 삭제할 수 있다")
    void deleteAuction_판매자가_경매를_성공적으로_삭제한다() {
        Long sellerId = 10L;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(sellerId);

        Auction auction = mock(Auction.class);
        when(auction.getUser()).thenReturn(owner);
        when(auction.getStatus()).thenReturn(AuctionStatus.SCHEDULED);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        auctionCommandService.deleteAuction(sellerId, 1L);

        verify(auction).markAsDeleted();
    }

    @Test
    @DisplayName("deleteAuction - 판매자가 아니면 삭제할 수 없다")
    void deleteAuction_판매자가_아니면_경매를_삭제할_수_없다() {
        Long sellerId = 10L;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);

        Auction auction = mock(Auction.class);
        when(auction.getUser()).thenReturn(owner);
        when(auction.getStatus()).thenReturn(AuctionStatus.SCHEDULED);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        assertThatThrownBy(() ->
                auctionCommandService.deleteAuction(sellerId, 1L)
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("deleteAuction - 진행중 또는 종료된 경매는 삭제할 수 없다")
    void deleteAuction_경매가_진행중이거나_종료된_상태라면_삭제할_수_없다() {
        Long userId = 10L;

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(userId);

        Auction auction = mock(Auction.class);
        when(auction.getUser()).thenReturn(owner);
        when(auction.getStatus()).thenReturn(AuctionStatus.ONGOING);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        assertThatThrownBy(() ->
                auctionCommandService.deleteAuction(userId, 1L)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.CANNOT_MODIFY_ONGOING_OR_CLOSED_AUCTION.getMessage());
    }

    @Test
    @DisplayName("closeAuction - 입찰이 없으면 CLOSED_FAILED 상태로 종료된다")
    void closeAuction_입찰이_없으면_경매가_CLOSED_FAILED로_종료된다() {
        Auction auction = mock(Auction.class);
        when(auction.canClose()).thenReturn(true);
        when(auction.getId()).thenReturn(1L);

        when(auctionRepository.findById(1L))
                .thenReturn(Optional.of(auction));

        when(bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction))
                .thenReturn(Optional.empty());

        auctionCommandService.closeAuction(1L);

        verify(auction).closeAsFailed();
        verify(auctionRepository).save(auction);
    }

    @Test
    @DisplayName("closeAuction - 입찰이 있으면 낙찰 처리되고 CLOSED 상태가 된다")
    void closeAuction_입찰이_존재하면_경매가_낙찰되고_CLOSED로_종료된다() {
        User bidder = mock(User.class);
        when(bidder.getId()).thenReturn(50L);

        Bid bid = mock(Bid.class);
        when(bid.getUser()).thenReturn(bidder);
        when(bid.getBidAmount()).thenReturn(2500L);

        Auction auction = mock(Auction.class);
        when(auction.canClose()).thenReturn(true);
        when(auction.getId()).thenReturn(1L);

        when(auctionRepository.findById(1L))
                .thenReturn(Optional.of(auction));

        when(bidQueryServiceApi.findTopByAuctionOrderByBidAmountDesc(auction))
                .thenReturn(Optional.of(bid));

        auctionCommandService.closeAuction(1L);

        verify(auction).assignWinner(50L, 2500L);
        verify(pointCommandService).depositToAvailablePoint(1L);
        verify(pointCommandService).depositToSettlement(1L);
        verify(auctionRepository).save(auction);
    }

    @Test
    @DisplayName("closeAuction - 이미 종료된 경매는 다시 종료할 수 없다")
    void closeAuction_이미_종료된_경매는_종료할_수_없다() {
        Auction auction = mock(Auction.class);

        when(auctionRepository.findById(1L))
                .thenReturn(Optional.of(auction));

        when(auction.canClose()).thenReturn(false);

        assertThatThrownBy(() ->
                auctionCommandService.closeAuction(1L)
        ).isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.AUCTION_ALREADY_CLOSED.getMessage());
    }

}
