package com.jobflow.service;

import com.jobflow.persistence.entity.OAuthLoginExchangeEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.OAuthLoginExchangeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OAuthLoginExchangeServiceTest {
    private static final String CLIENT_ID = "jobflow-web";
    private static final String VERIFIER = "abc_DEF-0123456789.abcdefghijklmnopqrstuvwxyz";
    private final OAuthLoginExchangeJpaRepository repository = mock(OAuthLoginExchangeJpaRepository.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private OAuthLoginExchangeService service;
    private String clearCode;

    @BeforeEach
    void setUp() {
        when(transactions.getTransaction(any(TransactionDefinition.class)))
                .thenAnswer(ignored -> new SimpleTransactionStatus());
        doNothing().when(transactions).commit(any());
        doNothing().when(transactions).rollback(any());
        service = new OAuthLoginExchangeService(repository, transactions);
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void issuesRandomCredentialAndPersistsOnlyItsHash() {
        var saved = org.mockito.ArgumentCaptor.forClass(OAuthLoginExchangeEntity.class);
        String credential = service.issue(user(), CLIENT_ID, challenge());

        verify(repository).saveAndFlush(saved.capture());
        assertNotEquals(credential, saved.getValue().getCredentialHash());
        assertEquals(OAuthLoginExchangeService.hash(credential), saved.getValue().getCredentialHash());
        assertEquals(CLIENT_ID, saved.getValue().getClientId());
        assertEquals(challenge(), saved.getValue().getVerifierChallenge());
        assertTrue(saved.getValue().getExpiresAt()
                .isAfter(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)));
    }

    @Test
    void rejectsBlankClientOnIssue() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(user(), " ", challenge()));
    }

    @Test
    void rejectsMalformedChallengeOnIssue() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(user(), CLIENT_ID, "bad"));
    }

    @Test
    void rejectsMissingUserOnIssue() {
        assertThrows(IllegalArgumentException.class, () -> service.issue(null, CLIENT_ID, challenge()));
    }

    @Test
    void consumesCredentialOnlyWithMatchingClientAndVerifier() {
        OAuthLoginExchangeEntity entry = issuedEntry();
        allowConsume(entry);

        assertSame(entry.getUser(), service.consume(clearCode, CLIENT_ID, VERIFIER));
        verify(repository).consume(entry.getCredentialHash(), CLIENT_ID, challenge());
    }

    @Test
    void rejectsWrongClientBinding() {
        OAuthLoginExchangeEntity entry = issuedEntry();
        when(repository.consume(anyString(), eq("different-client"), anyString())).thenReturn(0);

        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume("clear-code", "different-client", VERIFIER));
        verify(repository, never()).findByCredentialHash(anyString());
    }

    @Test
    void rejectsWrongVerifierBinding() {
        when(repository.consume(anyString(), eq(CLIENT_ID), anyString())).thenReturn(0);

        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume("clear-code", CLIENT_ID, "x".repeat(43)));
    }

    @Test
    void rejectsExpiredCredentialWhenAtomicConsumeFindsNoValidRow() {
        when(repository.consume(anyString(), eq(CLIENT_ID), eq(challenge()))).thenReturn(0);

        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume("expired-code", CLIENT_ID, VERIFIER));
    }

    @Test
    void rejectsReplayAfterCredentialWasConsumed() {
        OAuthLoginExchangeEntity entry = issuedEntry();
        allowConsume(entry);
        when(repository.consume(entry.getCredentialHash(), CLIENT_ID, challenge())).thenReturn(1, 0);

        service.consume(clearCode, CLIENT_ID, VERIFIER);
        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume(clearCode, CLIENT_ID, VERIFIER));
    }

    @Test
    void rejectsMalformedVerifierAndMissingValues() {
        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume(null, CLIENT_ID, VERIFIER));
        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume("code", CLIENT_ID, "invalid"));
        assertThrows(OAuthLoginExchangeService.ExchangeException.class,
                () -> service.consume("code", null, VERIFIER));
    }

    @Test
    void concurrentConsumersCanConsumeExchangeOnlyOnce() throws Exception {
        OAuthLoginExchangeEntity entry = issuedEntry();
        AtomicBoolean consumed = new AtomicBoolean();
        when(repository.consume(eq(entry.getCredentialHash()), eq(CLIENT_ID), eq(challenge())))
                .thenAnswer(ignored -> consumed.compareAndSet(false, true) ? 1 : 0);
        when(repository.findByCredentialHash(entry.getCredentialHash())).thenReturn(Optional.of(entry));
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var one = executor.submit(() -> consumeTogether(ready, start));
            var two = executor.submit(() -> consumeTogether(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, one.get(5, TimeUnit.SECONDS) + two.get(5, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private int consumeTogether(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start signal timed out.");
        try {
            service.consume(clearCode, CLIENT_ID, VERIFIER);
            return 1;
        } catch (OAuthLoginExchangeService.ExchangeException rejected) {
            return 0;
        }
    }

    private OAuthLoginExchangeEntity issuedEntry() {
        clearCode = service.issue(user(), CLIENT_ID, challenge());
        var captor = org.mockito.ArgumentCaptor.forClass(OAuthLoginExchangeEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void allowConsume(OAuthLoginExchangeEntity entry) {
        when(repository.consume(entry.getCredentialHash(), CLIENT_ID, challenge())).thenReturn(1);
        when(repository.findByCredentialHash(entry.getCredentialHash())).thenReturn(Optional.of(entry));
    }

    private static UserEntity user() {
        UserEntity user = new UserEntity("OAuth User", "oauth@example.test");
        user.setId(42);
        return user;
    }

    private static String challenge() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
