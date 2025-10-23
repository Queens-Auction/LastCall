package org.example.lastcall.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getProfile(@Auth AuthUser authUser) {
        UserProfileResponse dto = userService.getUserById(authUser.userId());
        return ApiResponse.success("내 정보 조회 성공", dto);
    }
}
