package com.jobflow.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record JobRequest(
        @NotBlank
        @Size(max = 100, message = "must be at most 100 characters")
        String title,
        @NotBlank
        @Size(max = 100, message = "must be at most 100 characters")
        String company,
        @NotBlank
        @Size(max = 100, message = "must be at most 100 characters")
        String location,
        @NotNull
        @PositiveOrZero
        Double salary
) {
}
