package org.example.lastcall.domain.point.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.auction.repository.AuctionRepository;
import org.example.lastcall.domain.point.dto.CreatePointRequest;
import org.example.lastcall.domain.point.dto.PointResponse;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PointService {

    private final PointLogRepository pointLogRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public PointResponse createPoint(Long userId, @Valid CreatePointRequest request) {
        return null;
    }

}
