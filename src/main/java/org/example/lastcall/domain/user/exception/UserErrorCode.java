package org.example.lastcall.domain.user.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.lastcall.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_DELETED(HttpStatus.GONE, "이미 탈퇴한 사용자입니다."),

    // 회원가입 / 중복 관련
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 프로필 수정 관련
    NO_FIELDS_TO_UPDATE(HttpStatus.BAD_REQUEST, "수정할 항목이 존재하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
