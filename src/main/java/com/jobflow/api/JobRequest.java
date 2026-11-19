package com.jobflow.api;

import com.jobflow.model.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record JobRequest(
        @NotBlank String title,
        @NotBlank String company,
        @NotBlank String location,
        @PositiveOrZero double salary
) {
    public Job toJob() {
        return new Job(title, company, location, salary);
    }

    public Job toJob(int id) {
        return new Job(id, title, company, location, salary);
    }
}
