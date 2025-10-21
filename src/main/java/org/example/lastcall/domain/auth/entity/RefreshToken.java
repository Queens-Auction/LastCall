package org.example.lastcall.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.auth.model.RefreshTokenStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RefreshTokenStatus status;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    private RefreshToken(Long userId, String token, RefreshTokenStatus status, LocalDateTime expiredAt) {
        this.userId = userId;
        this.token = token;
        this.status = status;
        this.expiredAt = expiredAt;
    }

    public static RefreshToken create(Long userId, String token, RefreshTokenStatus status, LocalDateTime expiredAt) {
        return new RefreshToken(userId, token, status, expiredAt);
    }

    public void revoke() {
        this.status = RefreshTokenStatus.REVOKED;
    }
}
