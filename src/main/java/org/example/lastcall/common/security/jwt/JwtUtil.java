package org.example.lastcall.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.lastcall.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {
    private final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofMinutes(30);
    private final Duration REFRESH_TOKEN_EXPIRATION = Duration.ofDays(7);
    private static final String ISSUER = "Queens-Auction";

    private final SecretKey key;
    private final JwtParser parser;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
        this.parser = Jwts.parser()
                .clockSkewSeconds(60)
                .verifyWith(key)
                .build();
    }

    // 독립 실행이나 JDBC 조회값으로 직접 토큰 만들 때 쓰는 보조 생성자
    public JwtUtil(String base64Secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.parser = Jwts.parser()
                .clockSkewSeconds(60)
                .verifyWith(key)
                .build();
    }

    // 기존 엔티티 기반 메서드 그대로 유지
    public String createAccessToken(User user) {
        return createAccessToken(user.getId(), user.getPublicId(), user.getUserRole().name());
    }

    // 새 오버로드: 엔티티 없이도 생성 가능
    public String createAccessToken(Long userId, UUID publicId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(publicId.toString())
                .claim("uid", userId)
                .claim("role", role)
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
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public Date getExpiration(String token) {
        return parser.parseSignedClaims(token).getPayload().getExpiration();
    }

    public Claims validateAndGetClaims(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public static void main(String[] args) throws Exception {
        String base64Secret = mustGetEnv("JWT_SECRET");
        String url  = mustGetEnv("DB_URL");
        String user = mustGetEnv("DB_USERNAME");
        String pass = mustGetEnv("DB_PASSWORD");

        String outPath = System.getenv().getOrDefault("RESULT_CSV", "/Users/dxxxwls/Desktop/result.csv");
        String query   = System.getenv().getOrDefault(
                "QUERY",
                "SELECT id, public_id, user_role FROM users WHERE deleted_at IS NULL LIMIT 100"
        );

        JwtUtil util = new JwtUtil(base64Secret);
        List<String> tokens = new ArrayList<>();

        // 필요시 드라이버 명시
        // Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");

                UUID publicId;
                Object pub = rs.getObject("public_id");
                if (pub instanceof byte[]) {
                    publicId = bytesToUUID((byte[]) pub);
                } else {
                    publicId = UUID.fromString(rs.getString("public_id"));
                }

                String role = rs.getString("user_role"); // ADMIN/USER

                String jwt = util.createAccessToken(id, publicId, role);
                tokens.add(jwt);
            }
        }

        if (tokens.isEmpty()) {
            throw new IllegalStateException("쿼리 결과가 비어 있어 토큰을 생성하지 못했습니다.");
        }

        // 결과 저장
        Files.createDirectories(Paths.get(outPath).toAbsolutePath().getParent());
        try (BufferedWriter w = new BufferedWriter(new FileWriter(outPath, false))) {
            for (String t : tokens) {
                w.write(t);
                w.newLine();
            }
        }

        System.out.println("토큰 " + tokens.size() + "개 저장 완료 → " + Paths.get(outPath).toAbsolutePath());
    }

    // ===== 유틸 =====
    static String mustGetEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("환경변수 " + key + " 이(가) 필요합니다.");
        return v;
    }

    static UUID bytesToUUID(byte[] b) {
        if (b.length != 16) throw new IllegalArgumentException("public_id byte length != 16");
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++)  msb = (msb << 8) | (b[i] & 0xff);
        for (int i = 8; i < 16; i++) lsb = (lsb << 8) | (b[i] & 0xff);
        return new UUID(msb, lsb);
    }
}
