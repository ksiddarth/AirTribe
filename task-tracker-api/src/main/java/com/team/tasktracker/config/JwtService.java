package com.team.tasktracker.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        byte[] keyBytes = Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
