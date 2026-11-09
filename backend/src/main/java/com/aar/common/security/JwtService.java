package com.aar.common.security;

import com.aar.common.config.AppProperties;
import com.aar.system.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "Thiếu app.jwt.secret. Đặt biến môi trường JWT_SECRET hoặc chạy với profile dev");
        }
        byte[] bytes = Decoders.BASE64.decode(secret);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài tối thiểu 32 byte sau khi giải mã Base64");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMinutes = props.jwt().expirationMinutes();
    }

    public String issue(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .claim("email", user.email())
            .claim("role", user.role().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact();
    }

    public Optional<AuthUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                Role.valueOf(claims.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expirationMinutes() {
        return expirationMinutes;
    }
}
