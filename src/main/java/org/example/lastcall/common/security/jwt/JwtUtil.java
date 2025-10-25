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
    //private final Key key;

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

    public String createAccessToken(User user)
    {
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
        String jti = java.util.UUID.randomUUID().toString();   // RT 식별자

        return Jwts.builder()
                .subject(user.getPublicId().toString())        // UUID만
                .id(jti)                                       // jti(토큰의 기본 키) 클라이언트가 RT보내면 JWT 파싱해서 jti 추출 jti가 있으으면 새 토큰 발급하고 삭제
                .issuer("Queens-Auction")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(REFRESH_TOKEN_EXPIRATION)))
                .signWith(key)
                .compact();
    }

//    private String createToken(User user, Duration duration) {
//        Instant now = Instant.now();
//
//        return Jwts.builder()
//                .subject(user.getPublicId().toString()) //UUID
//                .claim("uid", user.getId()) //id(Long)
//                .claim("role", user.getUserRole().name())
//                .issuer("Queens-Auction")
//                .issuedAt(Date.from(now))
//                .expiration(Date.from(now.plus(duration)))
//                .signWith(key)
//                .compact();
//    }

//    public Date getExpiration(String token) {
//        return Jwts.parser()
//                .verifyWith((SecretKey) key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .getExpiration();
//    }
//
//    public Claims validateAndGetClaims(String token) {
//        return Jwts.parser()
//                .verifyWith((SecretKey) key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//    }



    public Date getExpiration(String token) {
        return parser.parseSignedClaims(token).getPayload().getExpiration();
    }

    public Claims validateAndGetClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
