package org.example.lastcall.domain.pointlog;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class PointLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pointId;
    private Long bidId;
    private Long userId;


    private String description;

    private Long pointChange;

    private Long availablePointAfter;

    private Long depositPointAfter;

    private Long settlementPointAfter;

    private Long relatedAuctionId;

}
