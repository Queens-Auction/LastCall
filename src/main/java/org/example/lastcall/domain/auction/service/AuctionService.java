package org.example.lastcall.domain.auction.service;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auction.dto.request.AuctionCreateRequest;
import org.example.lastcall.domain.auction.dto.response.AuctionResponse;
import org.example.lastcall.domain.auction.exception.AuctionErrorCode;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.product.entity.Product;
import org.example.lastcall.domain.product.repository.ProductRepository;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public AuctionResponse createAuction(Long userId, AuctionCreateRequest request) {
        // 1. 상품 존재 여부 확인
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.PRODUCT_NOT_FOUND));

        // 2. 상품 소유자 검증
        if (!product.getUser().getId().equals(userId)) {
            throw new BusinessException(AuctionErrorCode.UNAUTHORIZED_SELLER);
        }

        return null; // 임시
    }
}
