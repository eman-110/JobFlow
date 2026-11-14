package com.jobflow.api;

import com.jobflow.model.JobStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull JobStatus status) {
}
