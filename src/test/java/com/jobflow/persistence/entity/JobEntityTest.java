package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobEntityTest {
    private CompanyEntity company() {
        return new CompanyEntity(42, "Northstar Labs", null, "Germany");
    }

    @Test
    void shouldInitializeValuesAndDefaultToSaved() {
        JobEntity job = new JobEntity(1, company(), "Java Developer", "Berlin", new BigDecimal("150000.00"));

        assertEquals(1, job.getId());
        assertEquals(42, job.getCompanyId());
        assertEquals("Java Developer", job.getTitle());
        assertEquals("Berlin", job.getLocation());
        assertEquals(new BigDecimal("150000.00"), job.getSalary());
        assertEquals(JobStatus.SAVED, job.getStatus());
    }

    @Test
    void shouldSupportTheFullConstructorAndZeroSalary() {
        JobEntity job = new JobEntity(3, company(), "Java Developer", "Berlin", BigDecimal.ZERO);

        assertEquals(3, job.getId());
        assertEquals(42, job.getCompanyId());
        assertEquals(BigDecimal.ZERO, job.getSalary());
        assertEquals(JobStatus.SAVED, job.getStatus());
    }

    @Test
    void shouldRejectMissingCompanyId() {
        assertThrows(IllegalArgumentException.class,
                () -> new JobEntity(null, null, "Java Developer", "Berlin", BigDecimal.ONE));
    }

    @Test
    void shouldSetCreatedAtOnPersistenceAndPreserveAnExistingValue() {
        JobEntity job = new JobEntity(1, company(), "Java Developer", "Berlin", BigDecimal.ONE);

        job.initializeCreatedAt();
        assertNotNull(job.getCreatedAt());

        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        job.setCreatedAt(createdAt);
        job.initializeCreatedAt();
        assertEquals(createdAt, job.getCreatedAt());
    }

    @Test
    void shouldRejectInvalidSalaryAndBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new JobEntity(1, company(), "Developer", "Berlin", new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,
                () -> new JobEntity(1, company(), " ", "Berlin", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class,
                () -> new JobEntity(1, company(), "Developer", "Berlin", null));
    }

    @Test
    void shouldMapToJobsTableAndStoreStatusAsText() throws Exception {
        Table table = JobEntity.class.getAnnotation(Table.class);
        Enumerated statusMapping = JobEntity.class.getDeclaredField("status")
                .getAnnotation(Enumerated.class);
        Column salary = JobEntity.class.getDeclaredField("salary").getAnnotation(Column.class);
        Column createdAt = JobEntity.class.getDeclaredField("createdAt").getAnnotation(Column.class);

        assertEquals("jobs", table.name());
        assertEquals(jakarta.persistence.EnumType.STRING, statusMapping.value());
        assertEquals(12, salary.precision());
        assertEquals(2, salary.scale());
        assertEquals(false, createdAt.updatable());
    }
}
