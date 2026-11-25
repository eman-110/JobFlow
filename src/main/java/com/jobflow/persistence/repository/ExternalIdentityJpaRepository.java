package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.ExternalIdentityEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ExternalIdentityJpaRepository extends JpaRepository<ExternalIdentityEntity, Long> {
    @Query("select i from ExternalIdentityEntity i join fetch i.user where i.issuer = :issuer and i.subject = :subject")
    Optional<ExternalIdentityEntity> findByIssuerAndSubject(
            @Param("issuer") String issuer, @Param("subject") String subject);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from ExternalIdentityEntity i where i.issuer = :issuer and i.subject = :subject")
    Optional<ExternalIdentityEntity> findByIssuerAndSubjectForUpdate(
            @Param("issuer") String issuer, @Param("subject") String subject);

    boolean existsByUser_IdAndIssuer(Integer userId, String issuer);
}
