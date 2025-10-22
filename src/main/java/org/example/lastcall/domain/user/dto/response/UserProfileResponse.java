package org.example.lastcall.domain.user.dto.response;

import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String email,
        AddressResponse detailAddress,
        String phoneNumber,
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

    public record AddressResponse(
            String address,
            String postcode,
            String detailAddress
    ) {}
}


