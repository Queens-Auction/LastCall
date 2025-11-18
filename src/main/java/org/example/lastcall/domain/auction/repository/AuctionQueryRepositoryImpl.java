package org.example.lastcall.domain.auction.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.auction.dto.response.AuctionReadAllResponse;
import org.example.lastcall.domain.auction.dto.response.MyParticipatedResponse;
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
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuctionQueryRepositoryImpl implements AuctionQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    private final QAuction a = QAuction.auction;
    private final QProduct p = QProduct.product;
    private final QProductImage i = QProductImage.productImage;
    private final QBid b = QBid.bid;

    // 내 최고 입찰가 서브쿼리
    private Expression<Long> myMaxBidSubquery(Long userId) {
        return JPAExpressions.select(b.bidAmount.max())
                .from(b)
                .where(b.auction.id.eq(a.id)
                        .and(b.user.id.eq(userId)));
    }

    // 썸네일 서브쿼리
    private Expression<String> thumbnailSubquery() {
        return JPAExpressions
                .select(i.imageKey)
                .from(i)
                .where(i.product.id.eq(a.product.id)
                        .and(i.imageType.eq(ImageType.THUMBNAIL))
                        .and(i.deleted.isFalse()))
                .limit(1);
    }

    // 삭제되지 않은 경매 조건
    // BooleanBuilder : 여러 조건을 동적으로 조립하기위한 QueryDSL 전용 객체
    private BooleanBuilder defaultAuctionCondition() {
        return new BooleanBuilder().and(a.deleted.isFalse());
    }

    // null-safe PageImpl 생성
    private <T> Page<T> toPage(List<T> results, Pageable pageable, Long total) {
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    // 경매 전체 조회
    @Override
    public Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable) {
        // 동적 필터 구성 (카테고리 + 기본 조건)
        BooleanBuilder whereBuilder = defaultAuctionCondition()
                .and(a.status.in(AuctionStatus.ONGOING, AuctionStatus.SCHEDULED));

        // category 필터가 있을 때만 카테고리 조건 추가
        if (category != null) {
            whereBuilder.and(p.category.eq(category));
        }

        NumberExpression<Long> participantCountExpr =
                Expressions.numberTemplate(
                        Long.class,
                        "({0})",
                        JPAExpressions
                                .select(b.user.id.countDistinct())
                                .from(b)
                                .where(b.auction.id.eq(a.id))
                );

        // 동적 정렬 조건 구성
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        pageable.getSort().forEach(order -> {
            String property = order.getProperty();  // 정렬 기준(필드)
            boolean asc = order.isAscending();      // 정렬 기준(오름/내림)

            switch (property) {
                case "createdAt" -> orderSpecifiers.add(asc ? a.createdAt.asc() : a.createdAt.desc());
                case "id" -> orderSpecifiers.add(asc ? a.id.asc() : a.id.desc());
                case "endTime" -> orderSpecifiers.add(asc ? a.endTime.asc() : a.endTime.desc());
                case "participantCount" ->
                        orderSpecifiers.add(asc ? participantCountExpr.asc() : participantCountExpr.desc());
            }
        });

        // 실제 데이터 조회
        List<AuctionReadAllResponse> results = jpaQueryFactory
                .select(Projections.constructor(AuctionReadAllResponse.class,
                        a.id,
                        ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
                        p.name,
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(b.user.id.countDistinct())
                                        .from(b)
                                        .where(b.auction.id.eq(a.id)),
                                "participantCount")))
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .orderBy(orderSpecifiers.toArray(new OrderSpecifier[0]))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 데이터 개수 조회 (페이징 total count)
        Long total = jpaQueryFactory
                .select(a.count())
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .fetchOne();

        return toPage(results, pageable, total);
    }

    // 내가 참여한 경매 목록 조회
    @Override
    public Page<MyParticipatedResponse> findMyParticipatedAuctions(Long userId, Pageable pageable) {
        // 사용자가 입찰한 경매 ID 목록 (페이징)
        List<Long> auctionIds = jpaQueryFactory
                .selectDistinct(b.auction.id)
                .from(b)
                .where(b.user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (auctionIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 참여 경매 정보 조회
        List<MyParticipatedResponse> results = jpaQueryFactory
                .select(Projections.constructor(MyParticipatedResponse.class,
                        a.id,
                        ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
                        p.name,
                        p.description,
                        a.currentBid,
                        a.status,
                        a.startTime,
                        a.endTime,
                        ExpressionUtils.as(
                                Expressions.booleanTemplate(
                                        "{0} = {1}",
                                        myMaxBidSubquery(userId),
                                        JPAExpressions.select(b.bidAmount.max())
                                                .from(b)
                                                .where(b.auction.id.eq(a.id))
                                ),
                                "isLeading"
                        ),
                        ExpressionUtils.as(myMaxBidSubquery(userId), "myBidAmount")
                ))
                .from(a)
                .join(a.product, p)
                .leftJoin(i).on(i.product.id.eq(p.id)
                        .and(i.imageType.eq(ImageType.THUMBNAIL))
                        .and(i.deleted.isFalse()))
                .leftJoin(b).on(b.auction.id.eq(a.id))
                .where(a.id.in(auctionIds), a.deleted.isFalse())
                .groupBy(a.id, i.imageKey, p.name, p.description,
                        a.status, a.startTime, a.endTime)
                .orderBy(a.createdAt.desc())
                .fetch();

        // 전체 데이터 개수 조회
        Long total = jpaQueryFactory
                .select(b.auction.id.countDistinct())
                .from(b)
                .where(b.user.id.eq(userId))
                .fetchOne();

        return toPage(results, pageable, total);
    }

    // 내가 참여한 경매 단건 조회
    @Override
    public Optional<MyParticipatedResponse> findMyParticipatedAuctionDetail(Long auctionId, Long userId) {

        BooleanBuilder participatedCondition = new BooleanBuilder()
                .and(JPAExpressions
                        .selectOne()
                        .from(b)
                        .where(
                                b.auction.id.eq(a.id),
                                b.user.id.eq(userId)
                        )
                        .exists()
                );

        MyParticipatedResponse result = jpaQueryFactory
                .select(Projections.constructor(MyParticipatedResponse.class,
                        a.id,
                        ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
                        p.name,
                        p.description,
                        a.currentBid,
                        a.status,
                        a.startTime,
                        a.endTime,
                        ExpressionUtils.as(
                                Expressions.booleanTemplate(
                                        "{0} = {1}",
                                        myMaxBidSubquery(userId),
                                        JPAExpressions.select(b.bidAmount.max())
                                                .from(b)
                                                .where(b.auction.id.eq(a.id))
                                ),
                                "isLeading"
                        ),
                        ExpressionUtils.as(myMaxBidSubquery(userId), "myBidAmount")
                ))
                .from(a)
                .join(a.product, p)
                .where(
                        a.id.eq(auctionId),
                        a.deleted.isFalse(),
                        participatedCondition
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
