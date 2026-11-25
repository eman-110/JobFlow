package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "oauth_link_intents")
public class OAuthLinkIntentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "intent_hash", nullable = false, unique = true, length = 64)
    private String intentHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;
    @Column(name = "verifier_challenge", nullable = false, length = 43)
    private String verifierChallenge;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    protected OAuthLinkIntentEntity() {}

    public OAuthLinkIntentEntity(String intentHash, UserEntity user, String clientId,
                                 String verifierChallenge, Instant expiresAt) {
        this.intentHash = intentHash;
        this.user = user;
        this.clientId = clientId;
        this.verifierChallenge = verifierChallenge;
        this.expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public String getIntentHash() { return intentHash; }
    public UserEntity getUser() { return user; }
    public String getClientId() { return clientId; }
    public String getVerifierChallenge() { return verifierChallenge; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
}
