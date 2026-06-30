package com.mulmi.backend.global.jwt;

import com.mulmi.backend.domain.user.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final Long accessExpiration;

    public JwtUtil(
            @Value("${jwt.token.secret-key}") String secretKey,
            @Value("${jwt.token.expiration.access}") Long accessExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
    }

    public String createAccessToken(Long userId, String loginId, UserRole role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("loginId", loginId)
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
}