package org.example.lastcall.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {
    private final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);
    private final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);
    private static final String ISSUER = "Queens-Auction";

    private final SecretKey key;
    private final JwtParser parser;

    public JwtUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.parser = Jwts.parser()
                .clockSkewSeconds(60)
                .verifyWith(key)
                .build();
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getPublicId().toString())
                .claim("uid", user.getId())
                .claim("role", user.getUserRole().name())
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TOKEN_EXPIRATION)))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(User user) {
        Instant now = Instant.now();
        String jti = java.util.UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(user.getPublicId().toString())
                .id(jti)
                .issuer("Queens-Auction")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(REFRESH_TOKEN_EXPIRATION)))
                .signWith(key)
                .compact();
    }

    public Date getExpiration(String token) {
        return parser.parseSignedClaims(token).getPayload().getExpiration();
    }

    public Claims validateAndGetClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
