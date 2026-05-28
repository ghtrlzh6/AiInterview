package com.aiinterview.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireSeconds;
    private final long refreshExpireSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expire}") long expireSeconds,
            @Value("${jwt.refresh-expire}") long refreshExpireSeconds) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = java.util.Arrays.copyOf(keyBytes, 32);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expireSeconds = expireSeconds;
        this.refreshExpireSeconds = refreshExpireSeconds;
    }

    public String generateAccessToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, expireSeconds, "access");
    }

    public String generateRefreshToken(Long userId, String username, String role) {
        return buildToken(userId, username, role, refreshExpireSeconds, "refresh");
    }

    private String buildToken(Long userId, String username, String role, long ttl, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", type);
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl * 1000))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type"));
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }
}
