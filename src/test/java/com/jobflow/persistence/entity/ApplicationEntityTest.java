package com.jobflow.persistence.entity;

import com.jobflow.model.JobStatus;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationEntityTest {

    @Test
    void shouldInitializeForeignKeysAndDefaultToApplied() {
        ApplicationEntity application = new ApplicationEntity(9, 3, 12);

        assertEquals(9, application.getId());
        assertEquals(3, application.getUserId());
        assertEquals(12, application.getJobId());
        assertEquals(JobStatus.APPLIED, application.getStatus());
    }

    @Test
    void shouldSetAppliedAtOnPersistenceAndPreserveAnExistingValue() {
        ApplicationEntity application = new ApplicationEntity(3, 12);

        application.initializeAppliedAt();
        assertNotNull(application.getAppliedAt());

        LocalDateTime appliedAt = LocalDateTime.of(2026, 3, 4, 5, 6);
        application.setAppliedAt(appliedAt);
        application.initializeAppliedAt();
        assertEquals(appliedAt, application.getAppliedAt());
    }

    @Test
    void shouldRejectMissingOrNonPositiveForeignKeys() {
        assertThrows(IllegalArgumentException.class, () -> new ApplicationEntity(null, 12));
        assertThrows(IllegalArgumentException.class, () -> new ApplicationEntity(3, 0));
    }

    @Test
    void shouldMapCompositeUniqueConstraint() {
        Table table = ApplicationEntity.class.getAnnotation(Table.class);
        UniqueConstraint unique = Arrays.stream(table.uniqueConstraints())
                .filter(constraint -> constraint.name().equals("uq_applications_user_job"))
                .findFirst()
                .orElseThrow();

        assertEquals("applications", table.name());
        assertEquals(Arrays.asList("user_id", "job_id"), Arrays.asList(unique.columnNames()));
    }
}
