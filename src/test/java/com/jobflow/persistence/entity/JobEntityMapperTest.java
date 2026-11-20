package com.jobflow.persistence.entity;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobEntityMapperTest {

    @Test
    void shouldMapDomainJobUsingCallerResolvedCompanyId() {
        Job job = new Job(7, "Java Developer", "Northstar Labs", "Berlin", 150000.25);

        JobEntity entity = JobEntityMapper.toEntity(job, 42);

        assertEquals(7, entity.getId());
        assertEquals(42, entity.getCompanyId());
        assertEquals(new BigDecimal("150000.25"), entity.getSalary());
        assertEquals(JobStatus.SAVED, entity.getStatus());
    }

    @Test
    void shouldMapEntityUsingCallerResolvedCompanyNameAndStatus() {
        JobEntity entity = new JobEntity(7, 42, "Java Developer", "Berlin", new BigDecimal("150000.25"));
        entity.setStatus(JobStatus.INTERVIEW);

        Job job = JobEntityMapper.toDomain(entity, "Northstar Labs");

        assertEquals(7, job.getId());
        assertEquals("Northstar Labs", job.getCompany());
        assertEquals(150000.25, job.getSalary());
        assertEquals(JobStatus.INTERVIEW, job.getStatus());
    }

    @Test
    void shouldPreserveRejectedStatusWhenMappingToDomain() {
        JobEntity entity = new JobEntity(8, 42, "Engineer", "London", BigDecimal.ONE);
        entity.setStatus(JobStatus.REJECTED);

        assertEquals(JobStatus.REJECTED, JobEntityMapper.toDomain(entity, "Cedar Analytics").getStatus());
    }
}
