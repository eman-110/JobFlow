package com.jobflow.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobTest {

    @ParameterizedTest
    @ValueSource(doubles = {-0.01, -100.0})
    void shouldRejectNegativeSalaries(double salary) {
        assertThrows(IllegalArgumentException.class,
                () -> new Job("Developer", "Example Co", "London", salary));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, Double.MIN_VALUE})
    void shouldAcceptBoundaryAndPositiveSalaries(double salary) {
        Job job = new Job("Developer", "Example Co", "London", salary);

        assertEquals(salary, job.getSalary());
    }
}
