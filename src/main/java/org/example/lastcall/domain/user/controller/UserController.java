package org.example.lastcall.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.common.response.ApiResponse;
import org.example.lastcall.common.security.Auth;
import org.example.lastcall.domain.auth.enums.AuthUser;
import org.example.lastcall.domain.auth.utils.CookieUtil;
import org.example.lastcall.domain.user.dto.request.PasswordChangeRequest;
import org.example.lastcall.domain.user.dto.request.UserUpdateRequest;
import org.example.lastcall.domain.user.dto.response.UserProfileResponse;
import org.example.lastcall.domain.user.service.UserService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원(User) API", description = "회원 정보 조회, 수정, 비밀번호 변경 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자의 프로필 정보를 조회합니다."
    )
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getProfile(@Auth AuthUser authUser) {
        UserProfileResponse dto = userService.getUserById(authUser.userId());
        return ApiResponse.success("내 정보 조회 성공", dto);
    }

    @Operation(
            summary = "내 정보 수정",
            description = "닉네임, 주소, 전화번호 등 사용자의 정보를 수정합니다."
    )
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@Auth AuthUser authUser,
                                                     @RequestBody UserUpdateRequest request) {
        UserProfileResponse response = userService.updateMyProfile(authUser.userId(), request);
        return ApiResponse.success("내 정보 수정 성공", response);
    }

    @Operation(
            summary = "비밀번호 변경",
            description = "로그인한 사용자가 비밀번호를 변경합니다. 변경 후 기존 세션은 모두 무효화됩니다."
    )
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
