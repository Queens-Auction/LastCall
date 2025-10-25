package org.example.lastcall.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;

@Schema(description = "사용자 프로필 응답 DTO")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "101")
        Long id,

        @Schema(description = "사용자 이름", example = "홍길동")
        String username,

        @Schema(description = "닉네임", example = "lastcaller")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "주소 상세 정보")
        AddressResponse detailAddress,

        @Schema(description = "전화번호", example = "01012345678")
        String phoneNumber,

        @Schema(description = "회원 역할", example = "USER")
        Role userRole
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                new AddressResponse(
                        user.getAddress(),
                        user.getPostcode(),
                        user.getDetailAddress()
                ),
                user.getPhoneNumber(),
                user.getUserRole()
        );
    }

    @Schema(description = "주소 정보 DTO")
    public record AddressResponse(
            @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
            String address,

            @Schema(description = "우편번호", example = "06236")
            String postcode,

            @Schema(description = "상세 주소", example = "101동 202호")
            String detailAddress
    ) {
    }
}


