package com.jobflow.controller;

import com.jobflow.api.JobRequest;
import com.jobflow.api.JobResponse;
import com.jobflow.model.Job;
import com.jobflow.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) {
        Job job = request.toJob();
        jobService.addJob(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(job));
    }

    @PutMapping("/{id}")
    public JobResponse updateJob(@PathVariable int id, @Valid @RequestBody JobRequest request) {
        return toResponse(jobService.updateJob(id, request.toJob(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable int id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
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
