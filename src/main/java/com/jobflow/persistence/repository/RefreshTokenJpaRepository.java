package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenEntity r join fetch r.user where r.tokenHash = :hash")
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshTokenEntity r where r.familyId = :family")
    java.util.List<RefreshTokenEntity> findByFamilyIdForUpdate(@Param("family") String family);
}
