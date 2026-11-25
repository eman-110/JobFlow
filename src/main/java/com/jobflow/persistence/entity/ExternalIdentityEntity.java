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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "external_identities", uniqueConstraints = @UniqueConstraint(
        name = "uq_external_identity_issuer_subject", columnNames = {"issuer", "subject"}))
public class ExternalIdentityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 255)
    private String issuer;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ExternalIdentityEntity() {}

    public ExternalIdentityEntity(UserEntity user, String issuer, String subject) {
        if (user == null || issuer == null || issuer.isBlank() || issuer.length() > 255
                || subject == null || subject.isBlank() || subject.length() > 255) {
            throw new IllegalArgumentException("A user, issuer, and subject are required.");
        }
        this.user = user;
        this.issuer = issuer;
        this.subject = subject;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getIssuer() { return issuer; }
    public String getSubject() { return subject; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
