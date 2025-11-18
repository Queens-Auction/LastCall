package org.example.lastcall.domain.auth.email.repository;

import org.example.lastcall.domain.auth.email.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findFirstByEmailOrderByCreatedAtDesc(String email);

    Optional<EmailVerification> findByPublicId(UUID publicId);
}

