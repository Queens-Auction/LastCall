package org.example.lastcall.domain.auction.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
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

@Repository // 서비스에 의존성 주입을 위해 필요 -> 단순 일반 클래스이기 때문에 자동으로 빈 인식X
@RequiredArgsConstructor
public class AuctionQueryRepositoryImpl implements AuctionQueryRepository {
    private final JPAQueryFactory jpaQueryFactory;

    // 공통 Q타입 필드 선언 //
    private final QAuction a = QAuction.auction;
    private final QProduct p = QProduct.product;
    private final QProductImage i = QProductImage.productImage;
    private final QBid b = QBid.bid;

    // 공통 메서드 //

    /**
     * [서브쿼리 1] 내 최고 입찰 금액
     * - 해당 경매에서 현재 로그인한 사용자가 입찰한 금액 중 최고가를 조회
     * - b.auction.id = a.id → 외부 쿼리와 연관
     */
    // 내 최고 입찰가 서브쿼리
    private Expression<Long> myMaxBidSubquery(Long userId) {
        return JPAExpressions.select(b.bidAmount.max())
                .from(b)
                .where(b.auction.id.eq(a.id)
                        .and(b.user.id.eq(userId)));
    }

    /**
     * [서브쿼리 2] 썸네일 이미지 URL
     * - product_image 테이블에서 썸네일(THUMBNAIL) 타입 이미지를 1건 조회
     * - limit(1) 적용으로 불필요한 중복 방지
     */
    // 썸네일 서브쿼리
    private Expression<String> thumbnailSubquery() {
        return JPAExpressions
                .select(i.imageUrl)
                .from(i)
                .where(i.product.id.eq(a.product.id)
                        .and(i.imageType.eq(ImageType.THUMBNAIL))
                        .and(i.deleted.isFalse()))
                .limit(1);
    }

    // 삭제되지 않은 경매 조건
    private BooleanBuilder defaultAuctionCondition() {
        return new BooleanBuilder().and(a.deleted.isFalse());
    }

    // null-safe PageImpl 생성
    private <T> Page<T> toPage(List<T> results, Pageable pageable, Long total) {
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }

    /**
     * 경매 전체 조회 (카테고리별 필터 + 동적 정렬)
     * 기본 정렬: 최신순 (createdAt desc, id desc)
     * 지원 정렬: endTime(마감임박순), participantCount(참여자순)
     */
    // 경매 전체 조회
    @Override
    public Page<AuctionReadAllResponse> findAllAuctionSummaries(Category category, Pageable pageable) {
        /**
         * 1️. 동적 필터 구성 (카테고리 + 기본 조건)
         *  - 삭제되지 않은 경매만 조회
         *  - 진행 중(ONGOING) 또는 예정된(SCHEDULED) 경매만 포함
         *  - 카테고리는 선택적으로 필터링
         */
        BooleanBuilder whereBuilder = defaultAuctionCondition()
                // 진행 중 또는 예정된 경매만
                .and(a.status.in(AuctionStatus.ONGOING, AuctionStatus.SCHEDULED));

        // 카테고리 필터 (선택적)
        if (category != null) {
            whereBuilder.and(p.category.eq(category));
        }

        /**
         * 2️. 동적 정렬 조건 구성
         *  - sort 파라미터가 없으면 → 기본 최신순(createdAt desc, id desc)
         *  - sort 파라미터가 있으면 → 지정된 필드 기준 정렬
         *  - 허용된 정렬 기준:
         *      - endTime: 마감 임박순
         *      - participantCount: 입찰 참여자순(인기순)
         */
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
        /**
         * 3. 정렬 조건 비어 있을 경우 대비
         *  - 예외적인 상황(NPE) 방지
         *  - 잘못된 sort 파라미터 등 비정상 요청 처리
         */
        if (orderSpecifiers.isEmpty()) {
            orderSpecifiers.add(a.createdAt.desc());
            orderSpecifiers.add(a.id.desc());
        }

        /**
         * 4️. 실제 데이터 조회
         *  - product는 join (N+1 방지)
         *  - 썸네일 이미지와 입찰 참여자 수는 각각 서브쿼리로 조회
         *  - orderBy에 동적 정렬 조건 적용 (1회만 호출)
         */
        List<AuctionReadAllResponse> results = jpaQueryFactory
                .select(Projections.constructor(AuctionReadAllResponse.class,
                        a.id,
                        // 썸네일 이미지 - 서브쿼리
                        ExpressionUtils.as(thumbnailSubquery(), "imageUrl"),
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
        /**
         * 5️. 전체 데이터 개수 조회 (페이징 total count)
         *  - 동일 조건(whereBuilder) 사용
         *  - product 조인 유지 (카테고리 조건 반영)
         */
        Long total = jpaQueryFactory
                .select(a.count())
                .from(a)
                .join(a.product, p)
                .where(whereBuilder)
                .fetchOne();
        /**
         * 6️. PageImpl 반환
         *  - results: 조회 결과
         *  - pageable: 요청 페이지 정보
         *  - total: 전체 개수(null-safe)
         */
        return toPage(results, pageable, total);
    }

    // 내가 참여한 경매 목록 조회
    @Override
    public Page<MyParticipatedResponse> findMyParticipatedAuctions(Long userId, Pageable pageable) {
        /**
         * 1️. 사용자가 입찰한 경매 ID 목록 (페이징 적용)
         *   - DISTINCT: 동일 경매에 여러 번 입찰해도 중복 제거
         *   - offset/limit 로 IN 절 크기 제어 → 대규모 IN 성능 저하 방지
         *   - count 쿼리 분리: 전체 개수는 아래에서 따로 계산
         */
        List<Long> auctionIds = jpaQueryFactory
                .selectDistinct(b.auction.id)
                .from(b)
                .where(b.user.id.eq(userId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 입찰 내역 없으면 빈 페이지 반환
        if (auctionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        /**
         * 2. 참여 경매 정보 조회 (JOIN 최소화 + 서브쿼리 기반)
         *   - JOIN 최소화: auction ↔ product만 조인
         *   - 이미지(THUMBNAIL), 입찰 금액은 left join 으로 조회
         *   - groupBy: 이미지, 상품명 등 중복 방지
         *   - N+1 문제 없음 (모두 단일 쿼리에서 해결)
         *   - 최대 입찰 금액(currentBid) 산출: b.bidAmount.max()
         */
        List<MyParticipatedResponse> results = jpaQueryFactory
                .select(Projections.fields(MyParticipatedResponse.class,
                        a.id,
                        i.imageUrl.as("imageUrl"),
                        p.name,
                        p.description,
                        b.bidAmount.max().as("currentBid"),
                        a.status,
                        a.startTime,
                        a.endTime
                ))
                .from(a)
                .join(a.product, p)
                .leftJoin(i).on(i.product.id.eq(p.id)
                        .and(i.imageType.eq(ImageType.THUMBNAIL))
                        .and(i.deleted.isFalse()))
                .leftJoin(b).on(b.auction.id.eq(a.id))    // 입찰 정보 조인
                .where(a.id.in(auctionIds), a.deleted.isFalse())
                .groupBy(a.id, i.imageUrl, p.name, p.description,
                        a.status, a.startTime, a.endTime) // 최신순 정렬
                .orderBy(a.createdAt.desc())
                .fetch();
        /**
         * 3. 전체 데이터 개수 조회
         *   - countDistinct: 동일 경매 중복 제거
         *   - 별도 쿼리로 페이징 total count 계산
         */
        Long total = jpaQueryFactory
                .select(b.auction.id.countDistinct())
                .from(b)
                .where(b.user.id.eq(userId))
                .fetchOne();
        /**
         * 4. PageImpl 반환
         *  - results: 조회 결과
         *  - pageable: 요청 페이지 정보
         *  - total: 전체 개수(null-safe)
         */
        return toPage(results, pageable, total);
    }

    /**
     * - 시도1. 서브쿼리 기반 작성
     * - 시도2. 조인 기반으로 수정 -> 중복 조회 해결 x
     * - 시도3. 서브쿼리 + 조인 기반으로 해결
     */
    // 내가 참여한 경매 단건 조회 //
    @Override
    public Optional<MyParticipatedResponse> findMyParticipatedAuctionDetail(Long auctionId, Long userId) {
        /**
         * [메인 쿼리]
         *  - Auction + Product JOIN
         *  - Bid, Image는 서브쿼리로 처리하여 JOIN 최소화
         *  - isLeading (내가 현재 최고 입찰자인지)도 서브쿼리로 boolean 계산
         */
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
                                // 내 최고 입찰가 매핑
                                ExpressionUtils.as(myMaxBidSubquery(userId), "myBidAmount"),
                                // 현재 최고 입찰자인지 여부 확인 (exists 로 boolean 반환)
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
                        .fetchOne()
        );
    }
}
