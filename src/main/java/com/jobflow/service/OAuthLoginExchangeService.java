package com.jobflow.service;

import com.jobflow.persistence.entity.OAuthLoginExchangeEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.OAuthLoginExchangeJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@Profile("mysql")
public class OAuthLoginExchangeService {
    private static final long LIFETIME_SECONDS = 90;
    private final OAuthLoginExchangeJpaRepository exchanges;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    public OAuthLoginExchangeService(OAuthLoginExchangeJpaRepository exchanges,
                                    PlatformTransactionManager transactionManager) {
        this.exchanges = exchanges;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public String issue(UserEntity user, String clientId, String challenge) {
        if (user == null || clientId == null || clientId.isBlank() || clientId.length() > 255
                || !validChallenge(challenge)) {
            throw new IllegalArgumentException("A valid client and PKCE challenge are required.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String credential = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        exchanges.saveAndFlush(new OAuthLoginExchangeEntity(hash(credential), user, clientId, challenge,
                Instant.now().plusSeconds(LIFETIME_SECONDS)));
        return credential;
    }

    public UserEntity consume(String credential, String clientId, String verifier) {
        if (credential == null || clientId == null || verifier == null || !validVerifier(verifier)) {
            throw new ExchangeException("Exchange credential is invalid or expired.");
        }
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        UserEntity user = transactions.execute(status -> {
            String credentialHash = hash(credential);
            if (exchanges.consume(credentialHash, clientId, challenge) != 1) {
                throw new ExchangeException("Exchange credential is invalid or expired.");
            }
            OAuthLoginExchangeEntity row = exchanges.findByCredentialHash(credentialHash)
                    .orElseThrow(() -> new ExchangeException("Exchange credential is invalid or expired."));
            return row.getUser();
        });
        if (user == null) throw new ExchangeException("Exchange credential is invalid or expired.");
        return user;
    }

    static boolean validChallenge(String challenge) {
        return challenge != null && challenge.matches("[A-Za-z0-9_-]{43}");
    }

    static boolean validVerifier(String verifier) {
        return verifier != null && verifier.matches("[A-Za-z0-9._~-]{43,128}");
    }

    static String hash(String credential) {
        return HexFormat.of().formatHex(sha256(credential));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public static class ExchangeException extends RuntimeException {
        public ExchangeException(String message) { super(message); }
    }
}
