package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.APPLIED;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    protected ApplicationEntity() {
    }

    public ApplicationEntity(Integer id, UserEntity user, JobEntity job) {
        this.id = id;
        setUser(user);
        setJob(job);
    }

    public ApplicationEntity(UserEntity user, JobEntity job) {
        this(null, user, job);
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
        return user == null ? null : user.getId();
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required.");
        }
        this.user = user;
    }

    public Integer getJobId() {
        return job == null ? null : job.getId();
    }

    public JobEntity getJob() {
        return job;
    }

    public void setJob(JobEntity job) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required.");
        }
        this.job = job;
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
