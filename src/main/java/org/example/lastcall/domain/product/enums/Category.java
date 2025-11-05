package org.example.lastcall.domain.product.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품 카테고리 구분 Enum")
public enum Category {
    // 패션 //
    @Schema(description = "남성 패션")
    FASHION_MEN,
    @Schema(description = "여성 패션")
    FASHION_WOMEN,
    @Schema(description = "키즈 패션")
    FASHION_KIDS,
    @Schema(description = "신발")
    SHOES,
    @Schema(description = "가방")
    BAG,
    @Schema(description = "액세서리")
    ACCESSORY,
    @Schema(description = "시계/주얼리")
    WATCH_JEWELRY,

    // 전자기기 //
    @Schema(description = "스마트폰")
    SMARTPHONE,
    @Schema(description = "태블릿")
    TABLET,
    @Schema(description = "노트북")
    LAPTOP,
    @Schema(description = "데스크탑")
    DESKTOP,
    @Schema(description = "카메라")
    CAMERA,
    @Schema(description = "가전제품")
    HOME_APPLIANCE,
    @Schema(description = "오디오")
    AUDIO,

    // 취미, 레저 //
    @Schema(description = "스포츠 용품")
    SPORTS,
    @Schema(description = "게임")
    GAMES,
    @Schema(description = "악기")
    MUSICAL_INSTRUMENT,
    @Schema(description = "피규어/콜렉터블")
    FIGURE_COLLECTIBLE,

    // 예술 //
    @Schema(description = "회화")
    ART_PAINTING,
    @Schema(description = "조각")
    ART_SCULPTURE,
    @Schema(description = "공예품")
    ART_CRAFT,
    @Schema(description = "사진")
    PHOTOGRAPHY,

    // 차량 //
    @Schema(description = "자동차")
    CAR,
    @Schema(description = "오토바이")
    MOTORCYCLE,
    @Schema(description = "자동차 부품")
    CAR_PART,

    // 인테리어 //
    @Schema(description = "가구")
    FURNITURE,
    @Schema(description = "조명")
    LIGHTING,
    @Schema(description = "홈데코")
    HOME_DECOR,

    // 도서, 음반 //
    @Schema(description = "도서")
    BOOK,
    @Schema(description = "CD/DVD")
    CD_DVD,

    // 생활용품 //
    @Schema(description = "주방용품")
    KITCHEN,
    @Schema(description = "욕실용품")
    BATH,
    @Schema(description = "침구류")
    BEDDING,

    // 식품 //
    @Schema(description = "식품")
    FOOD,
    @Schema(description = "건강보조식품")
    SUPPLEMENT,
    @Schema(description = "음료")
    BEVERAGE,

    // 한정판, 소장품 //
    @Schema(description = "콜렉터블")
    COLLECTIBLE,
    @Schema(description = "앤티크")
    ANTIQUE,
    @Schema(description = "리미티드 에디션")
    LIMITED_EDITION,

    // 티켓 //
    @Schema(description = "콘서트 티켓")
    TICKET_CONCERT,
    @Schema(description = "스포츠 티켓")
    TICKET_SPORTS,
    @Schema(description = "체험 티켓")
    TICKET_EXPERIENCE,

    // 기타 //
    @Schema(description = "반려동물 용품")
    PET,
    @Schema(description = "교육")
    EDUCATION,
    @Schema(description = "디지털 콘텐츠")
    DIGITAL
}
