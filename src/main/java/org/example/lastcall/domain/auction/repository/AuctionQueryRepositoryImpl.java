package org.example.lastcall.domain.auction.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
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
    // 특정 유저의 특정 경매의 최고 입찰가
    // 즉 내가 입찰한 특정 경매의 최고 입찰가를 찾기 위한 것.
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
    // 제네릭 : 어떤 타입이 오더라도 처리하기 위해 사용
    // -> 조회 결과를 Page 형태로 감싸 반환하되, total count가 null이 되더라도 안전하게 처리(null-safe)
    private <T> Page<T> toPage(List<T> results, Pageable pageable, Long total) {
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    // 경매 전체 조회
    // 정리하자면
    // 1. 기본옵션 (sort 미지정) : 최신순 조회 (같으면 id순)
    // 2. 마감임박순 (sort endTime, asc) : 마감임박순으로 조회 (최신순x)
    // 3. 인기순 (sort participantCount, desc) : 인기순으로 조회 (최신순x)
    // 4. 카테고리순 (카테고리는 필터, sort 미지정) : 해당 카테고리를 최신순으로 조회
    @Override
    public Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable) {
        // 1️. 동적 필터 구성 (카테고리 + 기본 조건)
        BooleanBuilder whereBuilder = defaultAuctionCondition()
                .and(a.status.in(AuctionStatus.ONGOING, AuctionStatus.SCHEDULED));

        // category 필터가 있을 때만 카테고리 조건 추가
        if (category != null) {
            whereBuilder.and(p.category.eq(category));
        }

        // 2️. 동적 정렬 조건 구성
        // 여러 정렬 기준(endTime, participantCount 등)이 들어올 수 있기 때문에
        // OrderSpecifier들을 List에 누적하여 관리한다.
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        if (pageable.getSort().isEmpty()) {
            orderSpecifiers.add(a.createdAt.desc());
            orderSpecifiers.add(a.id.desc());
        } else {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();  // 정렬 기준(필드)
                boolean asc = order.isAscending();      // 정렬 기준(오름/내림)

                switch (property) {
                    case "endTime" -> orderSpecifiers.add(asc ? a.endTime.asc() : a.endTime.desc());
                    case "participantCount" ->
                            orderSpecifiers.add(asc ? a.participantCount.asc() : a.participantCount.desc());
//                    case "participantCount" -> orderSpecifiers.add(
//                            asc ? Expressions.numberPath(Long.class, "participantCount").asc()
//                                    : Expressions.numberPath(Long.class, "participantCount").desc());
                }
            });
        }
//        // 3.정렬 조건 비어 있을 경우 대비 -> 중복 코드
//        if (orderSpecifiers.isEmpty()) {
//            orderSpecifiers.add(a.createdAt.desc());
//            orderSpecifiers.add(a.id.desc());
//        }

        // 4. 실제 데이터 조회
        // 실제 경매에 참여한 참여자 수 조회 (중복 유저 제외)
        List<AuctionReadAllResponse> results = jpaQueryFactory
                .select(Projections.constructor(AuctionReadAllResponse.class,
                        a.id,
                        ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
                        p.name,
                        ExpressionUtils.as(
                                JPAExpressions
                                        .select(b.id.countDistinct())
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

        // 5️. 전체 데이터 개수 조회 (페이징 total count)
        Long total = jpaQueryFactory
                .select(a.count())
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .fetchOne();

        // 6️. PageImpl 반환
        return toPage(results, pageable, total);
    }

    // 내가 참여한 경매 목록 조회
    @Override
    public Page<MyParticipatedResponse> findMyParticipatedAuctions(Long userId, Pageable pageable) {
        // 1️. 사용자가 입찰한 경매 ID 목록 (페이징 적용)
        // 왜 1,2단계로 가져오는지 이유
        // - 입찰 같은 1:n 구조는 join하면 레코드 중복 발생 (페이징 어긋남)
        // - 그룹바이 통한 페이징은 성능 나빠짐
        // 내가 입찰한 경매 목록을 id 기준으로 먼저 페이징 (비어있으면 빈페이지) - 1단계
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

        // 2. 참여 경매 정보 조회
        // id 목록 기반으로 실제 경매 정보 조회 - 2단계
        List<MyParticipatedResponse> results = jpaQueryFactory
                .select(Projections.fields(MyParticipatedResponse.class,
                        a.id,
                        i.imageKey.as("imageKey"),
                        p.name,
                        p.description,
                        b.bidAmount.max().as("currentBid"),
                        a.status,
                        a.startTime,
                        a.endTime))
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

        // 3. 전체 데이터 개수 조회
        Long total = jpaQueryFactory
                .select(b.auction.id.countDistinct())
                .from(b)
                .where(b.user.id.eq(userId))
                .fetchOne();

        // 4. PageImpl 반환
        return toPage(results, pageable, total);
    }

    // 내가 참여한 경매 단건 조회 (서브쿼리 + 조인 기반으로 해결)
    // 경매 참여 여부는 이미 서비스 단에서 검증됨
    // 메인 쿼리 : 경매 기본 정보 + 내가 참여한 경매 상세 조회
    // 서브 쿼리 : 내가 참여한 경매에서 최고 입찰가 여부
    @Override
    public Optional<MyParticipatedResponse> findMyParticipatedAuctionDetail(Long auctionId, Long userId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .select(Projections.fields(MyParticipatedResponse.class,
                                a.id.as("id"),
                                ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
                                p.name.as("productName"),
                                p.description.as("productDescription"),
                                a.currentBid,
                                a.status,
                                a.startTime,
                                a.endTime,
                                ExpressionUtils.as(myMaxBidSubquery(userId), "myBidAmount"),
                                ExpressionUtils.as(
                                        JPAExpressions.selectOne()
                                                .from(b)
                                                .where(b.auction.id.eq(a.id)
                                                        .and(b.user.id.eq(userId))
                                                        .and(b.bidAmount.eq(myMaxBidSubquery(userId))))
                                                .exists(),
                                        "isLeading")))
                        .from(a)
                        .join(a.product, p)
                        .where(a.id.eq(auctionId), a.deleted.isFalse())
                        .fetchOne());
    }
}
