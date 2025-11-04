package org.example.lastcall.domain.auction.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.entity.QAuction;
import org.example.lastcall.domain.auction.enums.AuctionStatus;
import org.example.lastcall.domain.bid.entity.QBid;
import org.example.lastcall.domain.product.entity.QProduct;
import org.example.lastcall.domain.product.entity.QProductImage;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.product.enums.ImageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository // 서비스에 의존성 주입을 위해 필요 -> 단순 일반 클래스이기 때문에 자동으로 빈 인식X
@RequiredArgsConstructor
public class AuctionQueryRepositoryImpl implements AuctionQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    // 경매 전체 조회

    /**
     * 경매 전체 조회 (카테고리별 필터 + 동적 정렬)
     * 기본 정렬: 최신순 (createdAt desc, id desc)
     * 지원 정렬: endTime(마감임박순), participantCount(참여자순)
     */
    @Override
    public Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable) {
        // QueryDSL Q타입 초기화
        QAuction a = QAuction.auction;
        QProduct p = QProduct.product;
        QProductImage i = QProductImage.productImage;
        QBid b = QBid.bid;

        // 동적 필터 구성 : 카테고리
        // -> 추후 확장 고려
        BooleanBuilder whereBuilder = new BooleanBuilder()
                // 삭제되지 않은 경매만
                .and(a.deleted.isFalse())
                // 진행 중 또는 예정된 경매만
                .and(a.status.in(AuctionStatus.ONGOING, AuctionStatus.SCHEDULED));

        // 카테고리 필터 (선택적)
        if (category != null) {
            whereBuilder.and(p.category.eq(category));
        }

        // 정렬 조건 동적 설정
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        // 1. sort 파라미터 없을 때 : 기본 최신순 정렬
        if (pageable.getSort().isEmpty()) {
            orderSpecifiers.add(a.createdAt.desc());
            orderSpecifiers.add(a.id.desc());
            // 2. sort 파라미터 있을 때 : 동적 정렬
        } else {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                boolean asc = order.isAscending();

                switch (property) {
                    // 마감 임박순
                    case "endTime" -> orderSpecifiers.add(asc ? a.endTime.asc() : a.endTime.desc());

                    // 인기순(입찰 참여자순)
                    case "participantCount" -> orderSpecifiers.add(
                            asc ? Expressions.numberPath(Long.class, "participantCount").asc()
                                    : Expressions.numberPath(Long.class, "participantCount").desc());
                }
            });
        }

        // 3. 모든 경우 대비 : 혹시라도 정렬 비어있으면 최신순 정렬
        // -> NPE 방지 및 비정상 입력 대비 (실제 발생할 일 거의 X)
        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(a.createdAt.desc());
            orderSpecifiers.add(a.id.desc());
        }

        // 데이터 조회
        List<AuctionReadAllResponse> results = jpaQueryFactory
                .select(Projections.constructor(AuctionReadAllResponse.class,
                        a.id,
                        // 썸네일 이미지 - 서브쿼리
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(i.imageUrl)
                                        .from(i)
                                        .where(
                                                i.product.id.eq(p.id)
                                                        .and(i.imageType.eq(ImageType.THUMBNAIL))
                                                        .and(i.deleted.isFalse())
                                        )
                                        .limit(1),
                                "imageUrl"
                        ),
                        p.name,
                        // 입찰 참여자수 - 서브쿼리
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(b.id.countDistinct())
                                        .from(b)
                                        .where(b.auction.id.eq(a.id)),
                                "participantCount"
                        )
                ))
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0])) // 동적 정렬 적용 (1회)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 데이터 개수 조회 - 페이징 total count
        Long total = jpaQueryFactory
                .select(a.count())
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .fetchOne();

        // PageImpl로 결과 반환
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
}
