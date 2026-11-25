package com.jobflow.service;

import com.jobflow.persistence.entity.OAuthLinkIntentEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.OAuthLinkIntentJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@Profile("mysql")
public class OAuthLinkIntentService {
    private final OAuthLinkIntentJpaRepository intents;
    private final TransactionTemplate transactions;
    private final SecureRandom random = new SecureRandom();

    public OAuthLinkIntentService(OAuthLinkIntentJpaRepository intents,
                                  PlatformTransactionManager transactionManager) {
        this.intents = intents;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    public String issue(UserEntity user, String clientId, String challenge) {
        if (user == null || clientId == null || clientId.isBlank()
                || !OAuthLoginExchangeService.validChallenge(challenge)) {
            throw new IllegalArgumentException("A valid client and PKCE challenge are required.");
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        intents.saveAndFlush(new OAuthLinkIntentEntity(OAuthLoginExchangeService.hash(secret), user, clientId,
                challenge, Instant.now().plusSeconds(300)));
        return secret;
    }

    public Integer consume(String secret, String clientId, String challenge) {
        if (secret == null) throw new OAuthLoginExchangeService.ExchangeException("Link intent is invalid or expired.");
        String hash = OAuthLoginExchangeService.hash(secret);
        Integer userId = transactions.execute(status -> {
            if (intents.consume(hash, clientId, challenge) != 1) {
                throw new OAuthLoginExchangeService.ExchangeException("Link intent is invalid or expired.");
            }
            OAuthLinkIntentEntity intent = intents.findByIntentHash(hash)
                    .orElseThrow(() -> new OAuthLoginExchangeService.ExchangeException("Link intent is invalid or expired."));
            return intent.getUser().getId();
        });
        if (userId == null) throw new OAuthLoginExchangeService.ExchangeException("Link intent is invalid or expired.");
        return userId;
    }
}
