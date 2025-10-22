package org.example.lastcall.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.lastcall.common.config.JwtProperties;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key key;

    private final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);
    private final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);

    public JwtUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createAccessToken(User user) {
        return createToken(user, ACCESS_TOKEN_EXPIRATION);
    }

    public String createRefreshToken(User user) {
        return createToken(user, REFRESH_TOKEN_EXPIRATION);
    }

    private String createToken(User user, Duration duration) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getPublicId().toString()) //UUID
                .claim("uid", user.getId()) //id(Long)
                .claim("role", user.getUserRole().name())
                .issuer("Queens-Auction")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(duration)))
                .signWith(key)
                .compact();
    }

    public Date getExpiration(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
