package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, Integer> {
    List<ApplicationEntity> findByUserId(Integer userId);

    List<ApplicationEntity> findByJobId(Integer jobId);

    Optional<ApplicationEntity> findByUserIdAndJobId(Integer userId, Integer jobId);
}
