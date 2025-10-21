package org.example.lastcall.domain.auction.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.lastcall.domain.auction.entity.Auction;

@Getter
@Builder
// DTO는 DB나 JPA 와 상관없기 때문에 클래스 단위로 붙이는 게 가독성 + 편의성 모두 좋음
public class AuctionReadAllResponse {
    private Long id;
    private String imageUrl;
    private String productName;
    private Integer participantCount;

    // 정적 팩토리 메서드 (from)
    public static AuctionReadAllResponse from(Auction auction, String imageUrl) {
        return AuctionReadAllResponse.builder()
                .id(auction.getId())
                .imageUrl(imageUrl)
                .productName(auction.getProduct().getName())
                .participantCount(auction.getParticipantCount())
                .build();
    }
}
