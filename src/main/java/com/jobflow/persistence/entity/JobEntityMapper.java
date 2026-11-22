package com.jobflow.persistence.entity;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;

import java.math.BigDecimal;
import java.util.Objects;

public final class JobEntityMapper {
    private JobEntityMapper() {
    }

    public static JobEntity toEntity(Job job, CompanyEntity company) {
        Objects.requireNonNull(job, "Job is required.");
        JobEntity entity = new JobEntity(
                job.getId(),
                company,
                job.getTitle(),
                job.getLocation(),
                BigDecimal.valueOf(job.getSalary())
        );
        entity.setStatus(job.getStatus());
        return entity;
    }

    public static Job toDomain(JobEntity entity) {
        Objects.requireNonNull(entity, "Job entity is required.");
        Job job = new Job(
                entity.getId(),
                entity.getTitle(),
                entity.getCompany().getName(),
                entity.getLocation(),
                entity.getSalary().doubleValue()
        );
        switch (entity.getStatus()) {
            case SAVED -> {
            }
            case APPLIED -> job.updateStatus(JobStatus.APPLIED);
            case INTERVIEW -> {
                job.updateStatus(JobStatus.APPLIED);
                job.updateStatus(JobStatus.INTERVIEW);
            }
            case REJECTED -> {
                job.updateStatus(JobStatus.APPLIED);
                job.updateStatus(JobStatus.REJECTED);
            }
        }
        return job;
    }
}
