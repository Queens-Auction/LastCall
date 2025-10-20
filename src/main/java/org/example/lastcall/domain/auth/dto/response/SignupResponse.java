package org.example.lastcall.domain.auth.dto.response;

public class SignupResponse {
    private final String username;
    private final String nickname;
    private final String password;
    private final String email;
    private final String phoneNumber;
    private final String address;
    private final String detailAddress;
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
