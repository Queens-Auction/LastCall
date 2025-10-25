package org.example.lastcall.domain.auth.email.service;

import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.email.dto.request.EmailVerificationSendRequest;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.lastcall.common.util.GeneratorUtil;
import org.example.lastcall.domain.auth.email.config.EmailConfig;
import org.example.lastcall.domain.auth.email.dto.request.VerifyEmailVerificationCodeDto;
import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.repository.EmailVerificationRepository;
import org.example.lastcall.domain.auth.email.util.VerificationCodeGenerator;
import org.example.lastcall.domain.user.repository.UserRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
        final String verificationCode = VerificationCodeGenerator.generateVerificationCode();

        EmailVerification emailVerification = EmailVerification.create(
                GeneratorUtil.generatePublicId(),
                verificationCode,
                request.email()
        );

        emailVerificationRepository.save(emailVerification);

        // TODO:: 하루에 메일 당 최대 10번 요청 가능, 한번 요청 후 30초 후 요청 가능
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

    @Transactional(readOnly = true)
    public void validateDuplicateEmail(final String email) {
        boolean existsAlreadyEmail = userRepository.existsByEmail(email);
        if (existsAlreadyEmail) {
            throw new BusinessException(EmailErrorCode.DUPLICATE_EMAIL);
        }
    }


    @Transactional
    public VerifyEmailVerificationCodeDto.Response verifyEmailVerificationCode(final VerifyEmailVerificationCodeDto.Request request) {
        EmailVerification emailVerification = emailVerificationRepository.findFirstByEmailOrderByCreatedAtDesc(request.email())
                .orElseThrow(() -> new RuntimeException("잘못된 요청입니다."));

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
