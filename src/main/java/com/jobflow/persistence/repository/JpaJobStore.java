package com.jobflow.persistence.repository;

import com.jobflow.model.Job;
import com.jobflow.persistence.entity.CompanyEntity;
import com.jobflow.persistence.entity.JobEntity;
import com.jobflow.persistence.entity.JobEntityMapper;
import com.jobflow.repository.JobStore;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
@Transactional
public class JpaJobStore implements JobStore {
    private static final String UNSPECIFIED_COUNTRY = "Unspecified";

    private final JobJpaRepository jobs;
    private final CompanyJpaRepository companies;

    public JpaJobStore(JobJpaRepository jobs, CompanyJpaRepository companies) {
        this.jobs = jobs;
        this.companies = companies;
    }

    @Override
    public void add(Job job) {
        CompanyEntity company = resolveCompany(job.getCompany());
        JobEntity entity = new JobEntity(
                null,
                company.getId(),
                job.getTitle(),
                job.getLocation(),
                BigDecimal.valueOf(job.getSalary())
        );
        entity.setStatus(job.getStatus());
        JobEntity saved = jobs.saveAndFlush(entity);
        job.assignPersistentId(saved.getId());
    }

    @Override
    public void save(Job job) {
        CompanyEntity company = resolveCompany(job.getCompany());
        JobEntity entity = JobEntityMapper.toEntity(job, company.getId());
        jobs.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findAll() {
        return jobs.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Job> findPage(Pageable pageable) {
        Page<JobEntity> result = jobs.findAll(pageable);
        List<Job> content = result.getContent().stream().map(this::toDomain).toList();
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findById(int id) {
        return jobs.findById(id).map(this::toDomain);
    }

    @Override
    public boolean deleteById(int id) {
        if (!jobs.existsById(id)) {
            return false;
        }
        jobs.deleteById(id);
        return true;
    }

    private CompanyEntity resolveCompany(String name) {
        return companies.findByNameIgnoreCase(name)
                .orElseGet(() -> companies.saveAndFlush(
                        new CompanyEntity(name, null, UNSPECIFIED_COUNTRY)));
    }

    private Job toDomain(JobEntity entity) {
        CompanyEntity company = companies.findById(entity.getCompanyId())
                .orElseThrow(() -> new IllegalStateException(
                        "Company " + entity.getCompanyId() + " for job " + entity.getId() + " was not found."));
        return JobEntityMapper.toDomain(entity, company.getName());
    }
}
