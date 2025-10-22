package org.example.lastcall.domain.auth.repository;

import org.example.lastcall.domain.auth.entity.RefreshToken;
import org.example.lastcall.domain.auth.model.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findByUserIdAndStatus(Long userId, RefreshTokenStatus status);

    Optional<RefreshToken> findByTokenAndStatus(String token, RefreshTokenStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
           set rt.status = :revoked,
               rt.expiredAt = CURRENT_TIMESTAMP
         where rt.userId = :userId
           and rt.status = :active
        """)
    int revokeAllActiveByUserId(@Param("userId") Long userId,
                                @Param("active") RefreshTokenStatus active,
                                @Param("revoked") RefreshTokenStatus revoked);
}
