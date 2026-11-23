package com.jobflow.api;

import com.jobflow.model.JobStatus;
import com.jobflow.persistence.entity.ApplicationEntity;

import java.time.LocalDateTime;

public record ApplicationResponse(
        Integer id,
        Integer jobId,
        JobStatus status,
        LocalDateTime appliedAt) {

    public static ApplicationResponse from(ApplicationEntity application) {
        return new ApplicationResponse(
                application.getId(),
                application.getJobId(),
                application.getStatus(),
                application.getAppliedAt());
    }
}
