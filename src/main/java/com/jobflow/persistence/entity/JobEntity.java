package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.SAVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "job")
    private List<ApplicationEntity> applications = new ArrayList<>();

    protected JobEntity() {
    }

    public JobEntity(Integer id, CompanyEntity company, String title, String location, BigDecimal salary) {
        this.id = id;
        setCompany(company);
        setTitle(title);
        setLocation(location);
        setSalary(salary);
    }

    public JobEntity(CompanyEntity company, String title, String location, BigDecimal salary) {
        this(null, company, title, location, salary);
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCompanyId() {
        return company == null ? null : company.getId();
    }

    public CompanyEntity getCompany() {
        return company;
    }

    public void setCompany(CompanyEntity company) {
        if (company == null) {
            throw new IllegalArgumentException("Company is required.");
        }
        this.company = company;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = requireText(title, 100, "Job title");
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = requireText(location, 150, "Job location");
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        if (salary == null || salary.signum() < 0) {
            throw new IllegalArgumentException("Salary must be non-negative.");
        }
        if (salary.precision() > 12 || salary.scale() > 2
                || salary.compareTo(new BigDecimal("9999999999.99")) > 0) {
            throw new IllegalArgumentException("Salary must fit DECIMAL(12, 2).");
        }
        this.salary = salary;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Job status is required.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ApplicationEntity> getApplications() {
        return applications;
    }

    public void addApplication(ApplicationEntity application) {
        if (application == null) {
            throw new IllegalArgumentException("Application is required.");
        }
        JobEntity previousJob = application.getJob();
        if (previousJob != null && previousJob != this) {
            previousJob.removeApplicationReference(application);
        }
        application.setJob(this);
        addApplicationReference(application);
    }

    void addApplicationReference(ApplicationEntity application) {
        if (!applications.contains(application)) {
            applications.add(application);
        }
    }

    void removeApplicationReference(ApplicationEntity application) {
        applications.remove(application);
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be non-blank and at most "
                    + maxLength + " characters.");
        }
        return value;
    }
}
