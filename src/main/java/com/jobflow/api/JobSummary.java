package com.jobflow.api;

import com.jobflow.model.JobStatus;

import java.math.BigDecimal;

public record JobSummary(
        Integer id,
        String title,
        String companyName,
        String location,
        BigDecimal salary,
        JobStatus status
) {
}
