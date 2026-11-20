package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_applications_user_job",
                columnNames = {"user_id", "job_id"}
        )
)
public class ApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "job_id", nullable = false)
    private Integer jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.APPLIED;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    protected ApplicationEntity() {
    }

    public ApplicationEntity(Integer id, Integer userId, Integer jobId) {
        this.id = id;
        setUserId(userId);
        setJobId(jobId);
    }

    public ApplicationEntity(Integer userId, Integer jobId) {
        this(null, userId, jobId);
    }

    @PrePersist
    void initializeAppliedAt() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive.");
        }
        this.userId = userId;
    }

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        if (jobId == null || jobId <= 0) {
            throw new IllegalArgumentException("Job ID must be positive.");
        }
        this.jobId = jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Application status is required.");
        }
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
