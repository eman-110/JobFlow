package com.jobflow.persistence.entity;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobEntityMapperTest {

    @Test
    void shouldMapDomainJobUsingCallerResolvedCompany() {
        Job job = new Job(7, "Java Developer", "Northstar Labs", "Berlin", 150000.25);
        CompanyEntity company = new CompanyEntity(42, "Northstar Labs", null, "Germany");

        JobEntity entity = JobEntityMapper.toEntity(job, company);

        assertEquals(7, entity.getId());
        assertEquals(company, entity.getCompany());
        assertEquals(42, entity.getCompanyId());
        assertEquals(new BigDecimal("150000.25"), entity.getSalary());
        assertEquals(JobStatus.SAVED, entity.getStatus());
    }

    @Test
    void shouldMapEntityUsingCallerResolvedCompanyNameAndStatus() {
        JobEntity entity = new JobEntity(7,
                new CompanyEntity(42, "Northstar Labs", null, "Germany"),
                "Java Developer", "Berlin", new BigDecimal("150000.25"));
        entity.setStatus(JobStatus.INTERVIEW);

        Job job = JobEntityMapper.toDomain(entity);

        assertEquals(7, job.getId());
        assertEquals("Northstar Labs", job.getCompany());
        assertEquals(150000.25, job.getSalary());
        assertEquals(JobStatus.INTERVIEW, job.getStatus());
    }

    @Test
    void shouldPreserveRejectedStatusWhenMappingToDomain() {
        JobEntity entity = new JobEntity(8,
                new CompanyEntity(42, "Cedar Analytics", null, "UK"),
                "Engineer", "London", BigDecimal.ONE);
        entity.setStatus(JobStatus.REJECTED);

        assertEquals(JobStatus.REJECTED, JobEntityMapper.toDomain(entity).getStatus());
    }
}
