package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.ApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, Integer> {
    List<ApplicationEntity> findByUser_Id(Integer userId);

    List<ApplicationEntity> findByJob_Id(Integer jobId);

    Optional<ApplicationEntity> findByUser_IdAndJob_Id(Integer userId, Integer jobId);

    boolean existsByUser_IdAndJob_Id(Integer userId, Integer jobId);

    @Query("select application from ApplicationEntity application "
            + "join fetch application.user user "
            + "join fetch application.job job "
            + "join fetch job.company "
            + "where lower(user.email) = lower(:email)")
    List<ApplicationEntity> findAllWithUserAndJobByUserEmail(@Param("email") String email);
}
