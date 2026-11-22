package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationEntityTest {
    private UserEntity user() {
        return new UserEntity(3, "Ada Lovelace", "ada@example.test");
    }

    private JobEntity job() {
        return new JobEntity(12,
                new CompanyEntity(7, "Northstar Labs", null, "Germany"),
                "Java Developer", "Berlin", java.math.BigDecimal.ONE);
    }

    @Test
    void shouldInitializeEntityReferencesAndDefaultToApplied() {
        ApplicationEntity application = new ApplicationEntity(9, user(), job());

        assertEquals(9, application.getId());
        assertEquals(3, application.getUserId());
        assertEquals(12, application.getJobId());
        assertEquals(JobStatus.APPLIED, application.getStatus());
    }

    @Test
    void shouldSetAppliedAtOnPersistenceAndPreserveAnExistingValue() {
        ApplicationEntity application = new ApplicationEntity(user(), job());

        application.initializeAppliedAt();
        assertNotNull(application.getAppliedAt());

        LocalDateTime appliedAt = LocalDateTime.of(2026, 3, 4, 5, 6);
        application.setAppliedAt(appliedAt);
        application.initializeAppliedAt();
        assertEquals(appliedAt, application.getAppliedAt());
    }

    @Test
    void shouldRejectMissingOrNonPositiveForeignKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ApplicationEntity(null, job()));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationEntity(user(), null));
    }

    @Test
    void shouldMapCompositeUniqueConstraintAndNonUpdatableAppliedAt() throws Exception {
        Table table = ApplicationEntity.class.getAnnotation(Table.class);
        Column appliedAt = ApplicationEntity.class.getDeclaredField("appliedAt")
                .getAnnotation(Column.class);
        UniqueConstraint unique = Arrays.stream(table.uniqueConstraints())
                .filter(constraint -> constraint.name().equals("uq_applications_user_job"))
                .findFirst()
                .orElseThrow();

        assertEquals("applications", table.name());
        assertEquals(Arrays.asList("user_id", "job_id"), Arrays.asList(unique.columnNames()));
        assertEquals(false, appliedAt.updatable());
    }
}
