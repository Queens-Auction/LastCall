package org.example.lastcall.domain.auth.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.common.util.GeneratorUtil;
import org.example.lastcall.domain.auth.email.config.EmailConfig;
import org.example.lastcall.domain.auth.email.dto.request.EmailVerificationSendRequest;
import org.example.lastcall.domain.auth.email.dto.request.VerifyEmailVerificationCodeDto;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.email.util.VerificationCodeGenerator;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;
    private final EmailVerificationRepository emailVerificationRepository;

    @Transactional
    public void sendEmailVerificationCode(final EmailVerificationSendRequest.Request request) {
        validateDuplicateEmail(request.email());

        final String verificationCode = VerificationCodeGenerator.generateVerificationCode();

        EmailVerification emailVerification = EmailVerification.create(
                GeneratorUtil.generatePublicId(),
                verificationCode,
                request.email());

        emailVerificationRepository.save(emailVerification);

        sendEmail(request.email(), verificationCode);
    }

    private void sendEmail(final String userEmail, final String verificationCode) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();

            mail.setTo(userEmail);
            mail.setSubject("LastCall! 인증 코드");
            mail.setText(verificationCode);

            javaMailSender.send(mail);
        } catch (MailException e) {
            log.error("이메일 발송 실패. 수신자: {}, 원인: {}", userEmail, e.getMessage(), e);
            throw new BusinessException(EmailErrorCode.MAIL_SEND_FAILED);
        }
    }

    @Transactional
    public void validateDuplicateEmail(final String email) {
        boolean existsInUser = userRepository.existsByEmail(email);

        if (existsInUser) {
            throw new BusinessException(EmailErrorCode.DUPLICATE_EMAIL);
        }
    }

    @Transactional
    public VerifyEmailVerificationCodeDto.Response verifyEmailVerificationCode(final VerifyEmailVerificationCodeDto.Request request) {
        EmailVerification emailVerification = emailVerificationRepository
                .findFirstByEmailOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new BusinessException(EmailErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        LocalDateTime createdAt = emailVerification.getCreatedAt();
        UUID verificationPublicId = emailVerification.getPublicId();

        validateExpiredVerificationCode(createdAt);

        emailVerification.validateVerificationCode(request.verificationCode());
        emailVerification.updateStatus(EmailVerificationStatus.VERIFIED);

        return new VerifyEmailVerificationCodeDto.Response(verificationPublicId);
    }

    private void validateExpiredVerificationCode(final LocalDateTime createdAt) {
        long compareRequestTime = Duration.between(createdAt, LocalDateTime.now()).getSeconds();

        if (compareRequestTime > EmailConfig.POSSIBLE_REQUEST_TIME) {
            throw new BusinessException(EmailErrorCode.EXPIRED);
        }
    }
}
