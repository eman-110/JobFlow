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
                company,
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
        JobEntity entity = JobEntityMapper.toEntity(job, company);
        jobs.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findAll() {
        return toDomain(jobs.findAllWithCompany());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Job> findPage(Pageable pageable) {
        Page<JobEntity> result = jobs.findAllWithCompany(pageable);
        List<Job> content = toDomain(result.getContent());
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findById(int id) {
        return jobs.findByIdWithCompany(id).map(JobEntityMapper::toDomain);
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
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Company name must not be blank.");
        }
        return companies.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Company '" + name + "' does not exist."));
    }

    private List<Job> toDomain(List<JobEntity> entities) {
        return entities.stream().map(JobEntityMapper::toDomain).toList();
    }
}
