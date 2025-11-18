package org.example.lastcall.fixture;

import org.example.lastcall.domain.point.entity.Point;
import org.example.lastcall.domain.point.entity.PointLog;
import org.example.lastcall.domain.point.enums.PointLogType;
import org.example.lastcall.domain.point.repository.PointLogRepository;
import org.example.lastcall.domain.point.repository.PointRepository;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestPointService {
    @Autowired
    PointRepository repository;

    @Autowired
    PointLogRepository pointLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Point create(Long auctionId, Long bidId, User user, Long point) {
        Point savedPoint = repository.save(Point.of(user, point));

        pointLogRepository.save(PointLog.of(
                savedPoint,
                user.getId(),
                PointLogType.EARN,
                "포인트 등록",
                point,
                auctionId,
                bidId));

        return savedPoint;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Point createFirstDeposit(Long auctionId, Long bidId, User user, Long userPoint, Long depositPoint) {
        Point savedPoint = create(auctionId, bidId, user, userPoint);

        savedPoint.updateDepositPoint(depositPoint);

        repository.save(savedPoint);

        pointLogRepository.save(PointLog.of(
                savedPoint,
                user.getId(),
                PointLogType.DEPOSIT,
                "입찰금 예치 처리",
                depositPoint,
                auctionId,
                bidId));

        return savedPoint;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Point createForBid(Long auctionId, Long bidId, User user, Long depositPoint) {

        Point savedPoint = repository.save(
                Point.of(user, depositPoint)
        );

        savedPoint.updateDepositPoint(depositPoint);
        repository.save(savedPoint);

        pointLogRepository.save(PointLog.of(
                savedPoint,
                user.getId(),
                PointLogType.DEPOSIT,
                "입찰금 예치",
                depositPoint,
                auctionId,
                bidId
        ));

        return savedPoint;
    }
}
