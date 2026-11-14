package com.jobflow.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SalaryUpdateRequest(
        @NotNull
        @PositiveOrZero
        Double salary
) {
}
