package com.jobflow.service;

import com.jobflow.persistence.entity.ApplicationEntity;
import com.jobflow.persistence.entity.JobEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.ApplicationJpaRepository;
import com.jobflow.persistence.repository.JobJpaRepository;
import com.jobflow.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("mysql")
@Transactional
public class JobApplicationService {
    private final UserJpaRepository users;
    private final JobJpaRepository jobs;
    private final ApplicationJpaRepository applications;

    public JobApplicationService(
            UserJpaRepository users,
            JobJpaRepository jobs,
            ApplicationJpaRepository applications) {
        this.users = users;
        this.jobs = jobs;
        this.applications = applications;
    }

    public ApplicationEntity apply(Integer userId, Integer jobId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("Job ID must be positive.");
        }

        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + userId + " does not exist."));
        JobEntity job = jobs.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job " + jobId + " does not exist."));
        if (applications.existsByUser_IdAndJob_Id(userId, jobId)) {
            throw new IllegalStateException(
                    "User " + userId + " has already applied for job " + jobId + ".");
        }

        return applications.saveAndFlush(new ApplicationEntity(user, job));
    }
}
