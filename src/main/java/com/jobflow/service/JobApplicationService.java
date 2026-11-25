package com.jobflow.service;

import com.jobflow.persistence.entity.ApplicationEntity;
import com.jobflow.persistence.entity.JobEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.repository.ApplicationJpaRepository;
import com.jobflow.persistence.repository.JobJpaRepository;
import com.jobflow.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

    @PreAuthorize("hasRole('ADMIN')")
    public ApplicationEntity apply(Integer userId, Integer jobId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + userId + " does not exist."));
        return createApplication(user, jobId);
    }

    @PreAuthorize("hasRole('USER')")
    public ApplicationEntity applyForAuthenticatedUserId(Integer userId, Integer jobId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Authenticated user ID must be positive.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !String.valueOf(userId).equals(jwtAuthentication.getToken().getSubject())) {
            throw new AccessDeniedException("Authenticated identity does not match the requested user.");
        }
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user does not exist."));
        return createApplication(user, jobId);
    }

    private ApplicationEntity createApplication(UserEntity user, Integer jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("Job ID must be positive.");
        }

        JobEntity job = jobs.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Job " + jobId + " does not exist."));
        if (applications.existsByUser_IdAndJob_Id(user.getId(), jobId)) {
            throw new IllegalStateException(
                    "User " + user.getId() + " has already applied for job " + jobId + ".");
        }

        return applications.saveAndFlush(new ApplicationEntity(user, job));
    }
}
