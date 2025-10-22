package org.example.lastcall.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.service.UserService;
import org.springframework.web.bind.annotation.*;

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

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@Auth AuthUser authUser,
                                                     @RequestBody UserUpdateRequest request)
    {
        UserProfileResponse response = userService.updateMyProfile(authUser.userId(), request);
        return ApiResponse.success("내 정보 수정 성공", response);
    }
}
