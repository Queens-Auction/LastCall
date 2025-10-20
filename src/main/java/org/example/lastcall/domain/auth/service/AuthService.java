package org.example.lastcall.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.config.PasswordEncoder;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.util.GeneratorUtil;
import org.example.lastcall.domain.auth.dto.request.SignupRequest;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.example.lastcall.domain.user.entity.User;
import org.example.lastcall.domain.user.enums.Role;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;

    private void validateEmailVerifiedStatus(final EmailVerificationStatus status) {
        if (!Objects.equals(status, EmailVerificationStatus.VERIFIED)) {
            throw new BusinessException(EmailErrorCode.NOT_VERIFIED);
        }
    }

    @Transactional
    public void signup(final SignupRequest request) {
        // 이메일 인증 요청 존재 여부
        EmailVerification emailVerification = emailVerificationRepository
                .findByPublicId(request.getVerificationPublicId())
                .orElseThrow(() -> new BusinessException(EmailErrorCode.NOT_REQUESTED));

        // 검증 상태 확인
        validateEmailVerifiedStatus(emailVerification.getStatus()); // VERIFIED 아니면 예외

        // 4) 인증 기록 소비 처리
        emailVerification.updateStatus(EmailVerificationStatus.CONSUMED);

        User user = User.createForSignUp(
                GeneratorUtil.generatePublicId(),
                request.getUsername(),
                request.getNickname(),
                emailVerification.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getAddress(),
                request.getPostcode(),
                request.getDetailAddress(),
                request.getPhoneNumber(),
                Role.USER
        );
        userRepository.save(user);
    }
}
