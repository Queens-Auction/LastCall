package org.example.lastcall.domain.auction.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.response.PageResponse;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.ProductServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService implements AuctionServiceApi {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ProductServiceApi productService;
    // product 엔티티를 DB 조회 없이 식별자 기반 참조하기 위해 사용 (경매 등록시)
    private final EntityManager em;

    // 경매 상태 분리
    private AuctionStatus determineStatus(AuctionCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(request.getStartTime())) {
            return AuctionStatus.SCHEDULED;
        } else if (now.isAfter(request.getEndTime())) {
            return AuctionStatus.CLOSED;
        } else {
            return AuctionStatus.ONGOING;
        }
    }

    // 경매 등록 //
    public AuctionCreateResponse createAuction(Long userId, AuctionCreateRequest request) {

        // 1. 상품 존재 여부 확인
        ProductResponse productResponse = productService.readProduct(request.getProductId());
        if (productResponse == null) {
            throw new BusinessException(AuctionErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 상품 소유자 검증
        if (!productResponse.getUserId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }

        // 3. 중복 경매 등록 방지
        if (auctionRepository.existsActiveAuction(request.getProductId())) {
            throw new BusinessException(AuctionErrorCode.DUPLICATE_AUCTION);
        }

        // 4. 경매 상태 결정
        AuctionStatus status = determineStatus(request);

        // 5. User 엔티티 조회
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER)
        );

        // 6. Product 엔티티 조회 없이 참조만 (SELECT X)
        // -> Product 엔티티 내 빌더가 id 제외 되어있어서 (클래스 단위 아닌 메서드 단위)
        Product product = em.getReference(Product.class, productResponse.getId());

        // 7. 경매 등록
        Auction auction = Auction.of(
                user,
                product,
                request,
                status
        );

        auctionRepository.save(auction);

        return AuctionCreateResponse.from(auction);
    }

    // 경매 전체 조회 //
    @Transactional(readOnly = true)
    public PageResponse<AuctionReadAllResponse> readAllAuctions(Pageable pageable) {

        // 1. 경매 목록 조회 (최신순)
        /* findAllByOrderByCreatedAtDesc() 사용하는 이유?
            - JPA 가 쿼리 자동 생성 해줌 (SELECT * FROM auction ORDER BY created_at DESC)
            - 기본 옵션인 최신 등록순으로 경매 목록 조회 가능                                    */
        Slice<Auction> auctions = auctionRepository.findAllByOrderByCreatedAtDesc(pageable);

    }
}
