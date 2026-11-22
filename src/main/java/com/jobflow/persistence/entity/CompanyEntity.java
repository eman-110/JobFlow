package com.jobflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies", uniqueConstraints = @UniqueConstraint(
        name = "uq_companies_name", columnNames = "name"))
public class CompanyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(nullable = false, length = 100)
    private String country;

    @OneToMany(mappedBy = "company")
    private List<JobEntity> jobs = new ArrayList<>();

    protected CompanyEntity() {
    }

    public CompanyEntity(Integer id, String name, String website, String country) {
        this.id = id;
        setName(name);
        setWebsite(website);
        setCountry(country);
    }

    public CompanyEntity(String name, String website, String country) {
        this(null, name, website, country);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, 100, "Company name");
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        if (website != null && website.length() > 255) {
            throw new IllegalArgumentException("Website must not exceed 255 characters.");
        }
        this.website = website;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = requireText(country, 100, "Country");
    }

    public List<JobEntity> getJobs() {
        return jobs;
    }

    public void addJob(JobEntity job) {
        if (job == null) {
            throw new IllegalArgumentException("Job is required.");
        }
        CompanyEntity previousCompany = job.getCompany();
        if (previousCompany != null && previousCompany != this) {
            previousCompany.removeJobReference(job);
        }
        job.setCompany(this);
        addJobReference(job);
    }

    void addJobReference(JobEntity job) {
        if (!jobs.contains(job)) {
            jobs.add(job);
        }
    }

    void removeJobReference(JobEntity job) {
        jobs.remove(job);
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must be non-blank and at most "
                    + maxLength + " characters.");
        }
        return value;
    }
}
