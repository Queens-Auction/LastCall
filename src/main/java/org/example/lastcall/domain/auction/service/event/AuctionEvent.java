package org.example.lastcall.domain.auction.service.event;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor  // JSON 역직렬화 시 기본 생성자 필요
@AllArgsConstructor // 전체 필드 한 번에 초기화할 수 있는 생성자 생성
@ToString           // 디버깅용 : 객체 내용을 문자열로 출력 가능
public class AuctionEvent implements Serializable {
    // 경매 종료 시 메시지로 전달될 데이터 구조 정의 ->
    private Long auctionId;                // 경매 ID
    private Long winnerId;                 // 낙찰자 ID
    private Long winningBid;               // 낙찰 금액
    private List<Long> failedBidderIds;    // 유찰자 목록
}
