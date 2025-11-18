package org.example.lastcall.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.util.UUID;

@Schema(description = "회원가입 요청 DTO")
@Getter
public class SignupRequest {
    @Schema(description = "이메일 인증 완료 후 발급된 공개 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotNull(message = "verificationPublicId는 필수입니다.")
    UUID verificationPublicId;

    @Schema(description = "사용자 닉네임 (중복 불가)", example = "lastcaller")
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @Schema(description = "사용자 이름", example = "홍길동")
    @NotBlank(message = "사용자 이름은 필수입니다.")
    private String username;

    @Schema(description = "비밀번호 (대문자, 소문자, 숫자, 특수문자 포함 10자 이상)", example = "Lastcall123!")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'<>,.?/]).{10,}$",
            message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 모두 포함해 10자 이상이어야 합니다."
    )
    private String password;

    @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @Schema(description = "우편 번호", example = "06234")
    @NotBlank(message = "우편 번호는 필수입니다.")
    private String postcode;

    @Schema(description = "상세 주소", example = "101동 1001호")
    @NotBlank(message = "상세 주소는 필수입니다.")
    private String detailAddress;

    @Schema(description = "휴대폰 번호 (010으로 시작하는 11자리)", example = "01012345678")
    @NotBlank(message = "핸드폰 번호는 필수입니다.")
    @Pattern(
            regexp = "^010\\d{8}$",
            message = "휴대폰 번호는 010으로 시작하고 11자리여야 합니다."
    )
    private String phoneNumber;
}
