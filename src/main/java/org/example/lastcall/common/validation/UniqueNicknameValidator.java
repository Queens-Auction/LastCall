package org.example.lastcall.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniqueNicknameValidator implements ConstraintValidator<UniqueNickname, String> {

    private final UserRepository userRepository;

    @Override
    public boolean isValid(String nickname, ConstraintValidatorContext context) {
        if (nickname == null || nickname.isBlank()) return false;
        return !userRepository.existsByNickname(nickname); // 닉네임 중복 여부 검사
    }
}
