package org.example.lastcall.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.model.AuthUser;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.example.lastcall.domain.user.dto.request.PasswordChangeRequest;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.service.UserService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

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

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Auth AuthUser authUser,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        userService.changeMyPassword(authUser.userId(), request);

        // 쿠키 삭제 (비밀번호 변경 후 모든 세션 무효화)
        ResponseCookie delAT = cookieUtil.deleteCookieOfAccessToken();
        ResponseCookie delRT = cookieUtil.deleteCookieOfRefreshToken();

        return ResponseEntity.ok()
                .header("Set-Cookie", delAT.toString())
                .header("Set-Cookie", delRT.toString())
                .body(ApiResponse.success("비밀번호가 변경되었습니다."));
    }

}
