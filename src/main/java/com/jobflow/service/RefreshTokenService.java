package com.jobflow.service;

import com.jobflow.persistence.entity.RefreshTokenEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

@Service
@Profile("mysql | test")
public class RefreshTokenService {
    private final java.util.Optional<RefreshTokenJpaRepository> repository;
    private final SecureRandom random = new SecureRandom();
    private final long refreshDays;
    private final TransactionTemplate transactions;

    @Autowired
    public RefreshTokenService(@Autowired(required = false) RefreshTokenJpaRepository repository,
                               @Value("${jobflow.jwt.refresh-token-days:7}") long refreshDays,
                               PlatformTransactionManager transactionManager) {
        this.repository = java.util.Optional.ofNullable(repository);
        this.refreshDays = refreshDays;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public String create(UserEntity user) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new IllegalStateException("Refresh credentials are unavailable."));
        String credential = generateCredential();
        String familyId = UUID.randomUUID().toString();
        RefreshTokenEntity entity = new RefreshTokenEntity(
                hash(credential), user, Instant.now().plusSeconds(refreshDays * 86400), familyId);
        transactions.executeWithoutResult(status -> repository.save(entity));
        return credential;
    }

    public RotatedUser rotate(String credential) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new RefreshTokenException("Refresh credentials are unavailable."));
        RotationOutcome outcome = transactions.execute(status -> rotateWithinTransaction(
                repository, hash(credential)));
        if (outcome.reusedFamilyId() != null) {
            transactions.executeWithoutResult(status -> revokeFamily(repository, outcome.reusedFamilyId()));
            throw new RefreshTokenException("Refresh credential reuse is not allowed.");
        }
        return new RotatedUser(outcome.user(), outcome.replacement());
    }

    private RotationOutcome rotateWithinTransaction(RefreshTokenJpaRepository repository, String tokenHash) {
        RefreshTokenEntity current = repository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new RefreshTokenException("Refresh credential is invalid."));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (current.getRevokedAt() != null) {
            throw new RefreshTokenException("Refresh credential has been revoked.");
        }
        if (current.getRotatedAt() != null) {
            return new RotationOutcome(null, null, current.getFamilyId());
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
        return new RotationOutcome(current.getUser(), replacement, null);
    }

    public void revoke(String credential) {
        RefreshTokenJpaRepository repository = this.repository.orElseThrow(
                () -> new RefreshTokenException("Refresh credentials are unavailable."));
        transactions.executeWithoutResult(status -> repository.findByTokenHashForUpdate(hash(credential))
                .ifPresent(RefreshTokenEntity::revoke));
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

    private void revokeFamily(RefreshTokenJpaRepository repository, String familyId) {
        for (RefreshTokenEntity token : repository.findByFamilyIdForUpdate(familyId)) {
            token.revoke();
        }
    }

    private String generateCredential() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotatedUser(UserEntity user, String replacement) {}
    private record RotationOutcome(UserEntity user, String replacement, String reusedFamilyId) {}

    public static class RefreshTokenException extends RuntimeException {
        public RefreshTokenException(String message) { super(message); }
    }
}
