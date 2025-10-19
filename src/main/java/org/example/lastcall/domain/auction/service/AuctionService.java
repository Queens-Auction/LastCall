package org.example.lastcall.domain.auction.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionCreateResponse;
import org.example.lastcall.domain.auction.entity.Auction;
import org.example.lastcall.domain.auction.entity.AuctionStatus;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.product.dto.response.ProductResponse;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.sevice.ProductServiceApi;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.repository.UserRepository;
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
    private final EntityManager em;

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

        // 3. 경매 상태 결정
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();

        AuctionStatus status;
        if (now.isBefore(startTime)) {
            status = AuctionStatus.SCHEDULED;
        } else if (now.isAfter(endTime)) {
            status = AuctionStatus.CLOSED;
        } else {
            status = AuctionStatus.ONGOING;
        }

        // 4. User 엔티티 조회
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER)
        );

        // 5. Product 엔티티 조회 없이 참조만 (SELECT X)
        // -> Product 엔티티 내 빌더가 id 제외 되어있어서 (클래스 단위 아닌 메서드 단위)
        Product product = em.getReference(Product.class, productResponse.getId());

        // 6. 경매 등록
        Auction auction = Auction.of(
                user,
                product,
                request,
                status
        );

        auctionRepository.save(auction);

        return AuctionCreateResponse.from(auction);
    }
}
