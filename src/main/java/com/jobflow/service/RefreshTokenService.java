package com.jobflow.service;

import com.jobflow.persistence.entity.RefreshTokenEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final java.util.Optional<RefreshTokenJpaRepository> repository;
    private final SecureRandom random = new SecureRandom();
    private final long refreshDays;

    @Autowired
    public RefreshTokenService(@Autowired(required = false) RefreshTokenJpaRepository repository,
                               @Value("${jobflow.jwt.refresh-token-days:7}") long refreshDays) {
        this.repository = java.util.Optional.ofNullable(repository);
        this.refreshDays = refreshDays;
    }

    @Transactional
    public String create(UserEntity user) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new IllegalStateException("Refresh credentials are unavailable."));
        String credential = generateCredential();
        String familyId = UUID.randomUUID().toString();
        RefreshTokenEntity entity = new RefreshTokenEntity(
                hash(credential), user, Instant.now().plusSeconds(refreshDays * 86400), familyId);
        repository.save(entity);
        return credential;
    }

    @Transactional
    public RotatedUser rotate(String credential) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new RefreshTokenException("Refresh credentials are unavailable."));
        RefreshTokenEntity current = repository.findByTokenHashForUpdate(hash(credential))
                .orElseThrow(() -> new RefreshTokenException("Refresh credential is invalid."));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (current.getRevokedAt() != null) {
            throw new RefreshTokenException("Refresh credential has been revoked.");
        }
        if (current.getRotatedAt() != null) {
            revokeFamily(current.getFamilyId());
            throw new RefreshTokenException("Refresh credential reuse is not allowed.");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new RefreshTokenException("Refresh credential has expired.");
        }

        String replacement = generateCredential();
        RefreshTokenEntity replacementEntity = new RefreshTokenEntity(
                hash(replacement), current.getUser(), Instant.now().plusSeconds(refreshDays * 86400),
                current.getFamilyId());
        repository.save(replacementEntity);
        current.rotateTo(replacementEntity.getTokenHash());
        return new RotatedUser(current.getUser(), replacement);
    }

    @Transactional
    public void revoke(String credential) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new RefreshTokenException("Refresh credentials are unavailable."));
        repository.findByTokenHashForUpdate(hash(credential)).ifPresent(RefreshTokenEntity::revoke);
    }

    public static String hash(String credential) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(credential.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value));
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash refresh credential.", exception);
        }
    }

    private void revokeFamily(String familyId) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new RefreshTokenException("Refresh credentials are unavailable."));
        for (RefreshTokenEntity token : repository.findByFamilyId(familyId)) {
            token.revoke();
        }
    }

    private String generateCredential() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotatedUser(UserEntity user, String replacement) {}

    public static class RefreshTokenException extends RuntimeException {
        public RefreshTokenException(String message) { super(message); }
    }
}
