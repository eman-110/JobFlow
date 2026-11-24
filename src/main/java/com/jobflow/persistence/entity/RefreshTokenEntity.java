package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RefreshTokenEntity() {}

    public RefreshTokenEntity(String tokenHash, UserEntity user, Instant expiresAt, String familyId) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
        this.familyId = familyId;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public UserEntity getUser() { return user; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void revoke() { revokedAt = LocalDateTime.now(ZoneOffset.UTC); }
    public LocalDateTime getRotatedAt() { return rotatedAt; }
    public void rotateTo(String replacementHash) {
        rotatedAt = LocalDateTime.now(ZoneOffset.UTC);
        replacedByHash = replacementHash;
    }
    public String getReplacedByHash() { return replacedByHash; }
    public String getFamilyId() { return familyId; }
}
