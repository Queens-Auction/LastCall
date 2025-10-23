package org.example.lastcall.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserUpdateRequest(
        String nickname,
        String phoneNumber,
        AddressRequest addressInfo
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AddressRequest(
            String address,
            String postcode,
            String detailAddress
    ) {}

    public boolean isEmpty() {
        boolean addressEmpty = (addressInfo == null) ||
                (addressInfo.address() == null &&
                        addressInfo.postcode() == null &&
                        addressInfo.detailAddress() == null);

        return nickname == null && phoneNumber == null && addressEmpty;
    }
}
