package org.example.lastcall.domain.auth.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.common.exception.BusinessException;
import org.example.lastcall.domain.auth.email.enums.EmailVerificationStatus;
import org.example.lastcall.domain.auth.email.exception.EmailErrorCode;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "email_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID publicId;

    @Column(nullable = false)
    private String verificationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailVerificationStatus status;

    @Column(nullable = false)
    private String email;

    private EmailVerification(
            UUID publicId,
            String verificationCode,
            String email) {
        this.publicId = publicId;
        this.verificationCode = verificationCode;
        this.status = EmailVerificationStatus.SENT;
        this.email = email;
    }

    public static EmailVerification create(
            UUID publicId,
            String verificationCode,
            String email) {
        return new EmailVerification(publicId, verificationCode, email);
    }

    public void updateStatus(EmailVerificationStatus status) {
        this.status = status;
    }

    public void validateVerificationCode(final String requestedVerificationCode) {
        if (!Objects.equals(requestedVerificationCode, verificationCode)) {
            throw new BusinessException(EmailErrorCode.CODE_MISMATCH);
        }
    }
}
