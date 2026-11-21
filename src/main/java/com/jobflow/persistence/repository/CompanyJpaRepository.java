package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyJpaRepository extends JpaRepository<CompanyEntity, Integer> {
    Optional<CompanyEntity> findByNameIgnoreCase(String name);
}
