package com.jobflow.controller;

import com.jobflow.api.JobResponse;
import com.jobflow.model.Job;
import com.jobflow.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public List<JobResponse> getAllJobs() {
        return jobService.getAllJobs().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable int id) {
        return jobService.findJobById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<JobResponse> searchJobs(@RequestParam String company) {
        return jobService.findJobsByCompany(company).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/high-paying")
    public List<JobResponse> getHighPayingJobs(@RequestParam double minimumSalary) {
        return jobService.getHighPayingJobs(minimumSalary).stream()
                .map(this::toResponse)
                .toList();
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(job.getId(), job.getTitle(), job.getCompany(), job.getSalary());
    }
}
