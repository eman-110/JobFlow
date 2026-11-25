package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.OAuthLinkIntentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OAuthLinkIntentJpaRepository extends JpaRepository<OAuthLinkIntentEntity, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update OAuthLinkIntentEntity i set i.consumedAt = CURRENT_TIMESTAMP "
            + "where i.intentHash = :hash and i.consumedAt is null and i.expiresAt > CURRENT_TIMESTAMP "
            + "and i.clientId = :clientId and i.verifierChallenge = :challenge")
    int consume(@Param("hash") String hash, @Param("clientId") String clientId,
                @Param("challenge") String challenge);

    @Query("select i from OAuthLinkIntentEntity i join fetch i.user where i.intentHash = :hash")
    Optional<OAuthLinkIntentEntity> findByIntentHash(@Param("hash") String hash);
}
