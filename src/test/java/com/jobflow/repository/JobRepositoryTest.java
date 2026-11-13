package com.jobflow.repository;

import com.jobflow.model.Job;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobRepositoryTest {

    @Test
    void shouldFindAllJobsAndFindById() {
        Job first = new Job(1, "Java Developer", "Google", "London", 120000);
        Job second = new Job(2, "Backend Developer", "Microsoft", "London", 130000);
        JobRepository repository = new JobRepository(List.of(first, second));

        assertEquals(List.of(first, second), repository.findAll());
        assertEquals(first, repository.findById(1).orElseThrow());
        assertFalse(repository.findById(999).isPresent());
    }

    @Test
    void shouldReturnAnUnmodifiableJobsList() {
        JobRepository repository = new JobRepository(List.of());

        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().clear());
    }
}
