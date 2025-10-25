package org.example.lastcall.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 수정 요청 DTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserUpdateRequest(
        @Schema(description = "닉네임", example = "lastcall_user99")
        String nickname,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phoneNumber,

        @Schema(description = "주소 정보")
        AddressRequest addressInfo) {
    public boolean isEmpty() {
        boolean addressEmpty = (addressInfo == null) ||
                (addressInfo.address() == null &&
                        addressInfo.postcode() == null &&
                        addressInfo.detailAddress() == null);

        return nickname == null && phoneNumber == null && addressEmpty;
    }

    @Schema(description = "주소 정보 DTO")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddressRequest(
            @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
            String address,

            @Schema(description = "우편번호", example = "06236")
            String postcode,

            @Schema(description = "상세 주소", example = "101동 202호")
            String detailAddress
    ) {
    }
}
