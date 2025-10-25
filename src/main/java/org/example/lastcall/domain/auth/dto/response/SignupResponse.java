package org.example.lastcall.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public class SignupResponse {
    @Schema(description = "사용자 이름", example = "홍길동")
    private final String username;

    @Schema(description = "닉네임", example = "lastcaller")
    private final String nickname;

    @Schema(description = "비밀번호 (암호화 전송됨)", example = "********")
    private final String password;

    @Schema(description = "이메일 주소", example = "test@example.com")
    private final String email;

    @Schema(description = "휴대폰 번호", example = "01012345678")
    private final String phoneNumber;

    @Schema(description = "기본 주소", example = "서울특별시 강남구 테헤란로 123")
    private final String address;

    @Schema(description = "상세 주소", example = "101동 1001호")
    private final String detailAddress;

    @Schema(description = "우편번호", example = "06234")
    private final String post;

    public SignupResponse(String username, String nickname, String password, String email, String phoneNumber, String address, String detailAddress, String post) {
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.detailAddress = detailAddress;
        this.post = post;
    }
}
