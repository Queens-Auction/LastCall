package org.example.lastcall.domain.auth.repository;

import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.model.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);

    Optional<RefreshToken> findByTokenAndStatus(String token, RefreshTokenStatus status);

}
