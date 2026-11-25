package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.OAuthLoginExchangeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthLoginExchangeJpaRepository extends JpaRepository<OAuthLoginExchangeEntity, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OAuthLoginExchangeEntity e set e.consumedAt = CURRENT_TIMESTAMP "
            + "where e.credentialHash = :hash and e.consumedAt is null and e.expiresAt > CURRENT_TIMESTAMP "
            + "and e.clientId = :clientId and e.verifierChallenge = :challenge")
    int consume(@Param("hash") String hash, @Param("clientId") String clientId,
                @Param("challenge") String challenge);

    @Query("select e from OAuthLoginExchangeEntity e join fetch e.user where e.credentialHash = :hash")
    Optional<OAuthLoginExchangeEntity> findByCredentialHash(@Param("hash") String hash);
}
