package org.example.lastcall.domain.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import org.example.lastcall.common.validation.UniqueNickname;

import java.util.UUID;

@Getter
public class SignupRequest {
    @NotNull(message = "verificationPublicId는 필수입니다.")
    UUID verificationPublicId;

    @NotBlank(message = "닉네임은 필수입니다.")
    @UniqueNickname
    private String nickname;

    @NotBlank(message = "사용자 이름은 필수입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{10,}$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 모두 포함해 10자 이상이어야 합니다."
    )
    private String password;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @NotBlank(message = "우편 번호는 필수입니다.")
    private String postcode;

    @NotBlank(message = "상세 주소는 필수입니다.")
    private String detailAddress;

    @NotBlank(message = "핸드폰 번호는 필수입니다.")
    @Pattern(
            regexp = "^010\\d{8}$",
            message = "휴대폰 번호는 010으로 시작하고 11자리여야 합니다."
    )
    private String phoneNumber;
}
