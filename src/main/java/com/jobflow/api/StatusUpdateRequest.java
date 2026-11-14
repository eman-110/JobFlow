package com.jobflow.api;

import com.jobflow.model.JobStatus;

public record StatusUpdateRequest(JobStatus status) {
}
