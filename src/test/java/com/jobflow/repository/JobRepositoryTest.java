package com.jobflow.repository;

import com.jobflow.model.Job;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldUpdateAndDeleteJobsById() {
        Job original = new Job(12, "Developer", "Example Co", "London", 100000);
        Job replacement = new Job(12, "Senior Developer", "Example Co", "London", 120000);
        JobRepository repository = new JobRepository(List.of(original));

        assertTrue(repository.update(replacement));
        assertEquals(replacement, repository.findById(12).orElseThrow());
        assertFalse(repository.update(new Job(13, "Analyst", "Example Co", "London", 90000)));

        assertTrue(repository.deleteById(12));
        assertFalse(repository.deleteById(12));
        assertEquals(List.of(), repository.findAll());
    }
}
