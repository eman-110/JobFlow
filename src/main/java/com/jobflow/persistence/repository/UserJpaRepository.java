package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    List<UserEntity> findByNameContainingIgnoreCase(String name);
}
