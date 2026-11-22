package com.team.tasktracker.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "revoked_tokens", indexes = {
    @Index(name = "idx_revoked_tokens_jti", columnList = "jti")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
