package org.example.lastcall.domain.auction;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
import org.example.lastcall.domain.auction.dto.response.MySellingResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.auction.service.query.AuctionQueryService;
import org.example.lastcall.domain.bid.service.query.BidQueryServiceApi;
import org.example.lastcall.domain.product.dto.response.ProductImageResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.service.query.ProductQueryServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuctionQueryServiceTest {
    @InjectMocks
    private AuctionQueryService auctionQueryService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ProductQueryServiceApi productQueryServiceApi;

    @Mock
    private BidQueryServiceApi bidQueryServiceApi;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("전체 경매 조회 성공")
    void getAllAuctions_전체_경매_조회가_성공적으로_수행된다() {
        Page<AuctionReadAllResponse> page = new PageImpl<>(
                List.of(mock(AuctionReadAllResponse.class)),
                PageRequest.of(0, 10),
                1
        );

        when(auctionRepository.findAllAuctionSummaries(eq(Category.ACCESSORY), any()))
                .thenReturn(page);

        PageResponse<AuctionReadAllResponse> response =
                auctionQueryService.getAllAuctions(Category.ACCESSORY, PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        assertThat(response.getContent().size()).isEqualTo(1);
    }

    @Test
    @DisplayName("단건 경매 조회 성공")
    void getAuction_단건_경매_조회가_성공적으로_수행된다() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(99L);

        Product product = Product.of(
                owner,
                "테스트 상품",
                Category.FOOD,
                "테스트 설명"
        );
        ReflectionTestUtils.setField(product, "id", 100L);

        AuctionCreateRequest request = new AuctionCreateRequest();
        ReflectionTestUtils.setField(request, "startingBid", 10000L);
        ReflectionTestUtils.setField(request, "bidStep", 1000L);
        ReflectionTestUtils.setField(request, "startTime", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(request, "endTime", LocalDateTime.now().plusHours(1));

        Auction auction = Auction.of(owner, product, request);
        ReflectionTestUtils.setField(auction, "id", 1L);

        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.of(auction));

        AuctionReadResponse response =
                auctionQueryService.getAuction(1L, null);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("테스트 상품", response.getProductName());
        assertEquals(10000L, response.getStartingBid());
    }

    @Test
    @DisplayName("내가 참여한 전체 경매 조회 성공")
    void getMyParticipatedAuctions_내가_참여한_전체_경매_조회가_성공적으로_수행된다() {
        MyParticipatedResponse dto = new MyParticipatedResponse(
                1L,
                "test.jpg",
                "테스트 상품",
                "테스트 설명",
                2000L,
                AuctionStatus.ONGOING,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                false,
                null
        );

        Page<MyParticipatedResponse> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 10),
                1
        );

        when(auctionRepository.findMyParticipatedAuctions(eq(10L), any()))
                .thenReturn(page);

        PageResponse<MyParticipatedResponse> response =
                auctionQueryService.getMyParticipatedAuctions(10L, PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        assertThat(response.getContent().size()).isEqualTo(1);

        MyParticipatedResponse result = response.getContent().get(0);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProductName()).isEqualTo("테스트 상품");
        assertThat(result.getCurrentBid()).isEqualTo(2000L);
        assertThat(result.getStatus()).isEqualTo(AuctionStatus.ONGOING);
    }

    @Test
    @DisplayName("내가 참여한 경매 단건 조회 성공")
    void getMyParticipatedDetailAuction_내가_참여한_단건_경매_조회가_성공적으로_수행된다() {
        MyParticipatedResponse dto = new MyParticipatedResponse(
                1L,
                "test.jpg",
                "테스트 상품",
                "테스트 설명",
                3000L,
                AuctionStatus.ONGOING,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                false,
                2500L
        );

        when(auctionRepository.findMyParticipatedAuctionDetail(1L, 10L))
                .thenReturn(Optional.of(dto));

        MyParticipatedResponse response =
                auctionQueryService.getMyParticipatedDetailAuction(10L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductName()).isEqualTo("테스트 상품");
        assertThat(response.getCurrentBid()).isEqualTo(3000L);
        assertThat(response.getMyBidAmount()).isEqualTo(2500L);
        assertThat(response.getStatus()).isEqualTo(AuctionStatus.ONGOING);
    }

    @Test
    @DisplayName("내가 판매한 전체 경매 조회 성공")
    void getMySellingAuctions_내가_판매한_전체_경매_조회가_성공적으로_수행된다() {
        MySellingResponse dto = new MySellingResponse(
                1L,
                "test.jpg",
                "테스트 상품",
                "테스트 설명",
                3000L,
                AuctionStatus.ONGOING,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );

        Page<MySellingResponse> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 10),
                1
        );

        when(auctionRepository.findMySellingAuctions(eq(10L), any()))
                .thenReturn(page);

        PageResponse<MySellingResponse> response =
                auctionQueryService.getMySellingAuctions(10L, PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        assertThat(response.getContent().size()).isEqualTo(1);

        MySellingResponse item = response.getContent().get(0);
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getProductName()).isEqualTo("테스트 상품");
        assertThat(item.getImageUrl()).isEqualTo("test.jpg");
    }

    @Test
    @DisplayName("내가 판매한 단건 경매 조회 성공")
    void getMySellingDetailAuction_내가_판매한_전체_경매_조회가_성공적으로_수행된다() {
        Auction auction = mock(Auction.class);
        Product product = mock(Product.class);

        when(auction.getId()).thenReturn(1L);
        when(auction.getProduct()).thenReturn(product);
        when(auction.getStartTime()).thenReturn(LocalDateTime.now());
        when(auction.getEndTime()).thenReturn(LocalDateTime.now().plusDays(1));
        when(auction.getStartingBid()).thenReturn(10000L);
        when(auction.getBidStep()).thenReturn(1000L);
        when(auction.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(auction.getModifiedAt()).thenReturn(LocalDateTime.now());

        when(product.getId()).thenReturn(100L);
        when(product.getName()).thenReturn("테스트 상품");
        when(product.getDescription()).thenReturn("상품 설명");

        when(auctionRepository.findBySellerIdAndAuctionId(10L, 1L))
                .thenReturn(Optional.of(auction));

        when(productQueryServiceApi.findThumbnailImage(100L))
                .thenReturn(new ProductImageResponse(
                        1L, 100L, null, "thumb.jpg", null, null
                ));

        when(bidQueryServiceApi.findCurrentBidAmount(1L))
                .thenReturn(2000L);

        MySellingResponse response =
                auctionQueryService.getMySellingDetailAuction(10L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImageUrl()).isEqualTo("thumb.jpg");
        assertThat(response.getCurrentBid()).isEqualTo(2000L);
        assertThat(response.getProductName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("단건 경매 조회 시 경매 없으면 예외 발생")
    void getAuction_단건_경매_조회_시_경매가_존재하지_않으면_예외가_발생한다() {
        when(auctionRepository.findActiveById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                auctionQueryService.getAuction(1L, null)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("내가 참여한 경매 단건 조회에서 참여하지 않은 경매 조회 시 예외 발생")
    void getMyParticipatedDetailAuction_내가_참여한_단건_경매_조회에서_경매가_존재하지_않으면_예외가_발생한다() {
        when(auctionRepository.findMyParticipatedAuctionDetail(1L, 10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                auctionQueryService.getMyParticipatedDetailAuction(10L, 1L)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("내가 판매한 경매 단건 조회에서 판매하지 않은 경매 조회 시 예외 발생")
    void getMySellingDetailAuction_내가_판매한_단건_경매_조회에서_경매가_존재하지_않으면_예외가_발생한다() {
        when(auctionRepository.findBySellerIdAndAuctionId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                auctionQueryService.getMySellingDetailAuction(10L, 1L)
        )
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(AuctionErrorCode.AUCTION_NOT_FOUND.getMessage());
    }
}
