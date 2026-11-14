package com.jobflow.api;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;

public record JobResponse(
        int id,
        String title,
        String company,
        String location,
        double salary,
        JobStatus status
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany(),
                job.getLocation(),
                job.getSalary(),
                job.getStatus()
        );
    }
}
