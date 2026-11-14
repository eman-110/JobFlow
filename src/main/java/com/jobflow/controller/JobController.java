package com.jobflow.controller;

import com.jobflow.api.JobRequest;
import com.jobflow.api.JobResponse;
import com.jobflow.api.SalaryUpdateRequest;
import com.jobflow.api.StatusUpdateRequest;
import com.jobflow.model.Job;
import com.jobflow.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(@RequestBody JobRequest request) {
        Job job = jobService.createJob(
                request.title(),
                request.company(),
                request.location(),
                request.salary()
        );
        return ResponseEntity.created(URI.create("/api/jobs/" + job.getId()))
                .body(JobResponse.from(job));
    }

    @GetMapping
    public List<JobResponse> getAllJobs() {
        return jobService.getAllJobs().stream()
                .map(JobResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public JobResponse getJobById(@PathVariable int id) {
        return JobResponse.from(jobService.getJobById(id));
    }

    @PutMapping("/{id}")
    public JobResponse updateJob(@PathVariable int id, @RequestBody JobRequest request) {
        Job job = jobService.updateJob(
                id,
                request.title(),
                request.company(),
                request.location(),
                request.salary()
        );
        return JobResponse.from(job);
    }

    @PatchMapping("/{id}/salary")
    public JobResponse updateSalary(@PathVariable int id, @RequestBody SalaryUpdateRequest request) {
        return JobResponse.from(jobService.updateSalary(id, request.salary()));
    }

    @PatchMapping("/{id}/status")
    public JobResponse updateStatus(@PathVariable int id, @RequestBody StatusUpdateRequest request) {
        return JobResponse.from(jobService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable int id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<JobResponse> searchJobs(@RequestParam String company) {
        return jobService.findJobsByCompany(company).stream()
                .map(JobResponse::from)
                .toList();
    }

    @GetMapping("/high-paying")
    public List<JobResponse> getHighPayingJobs(@RequestParam double minimumSalary) {
        return jobService.getHighPayingJobs(minimumSalary).stream()
                .map(JobResponse::from)
                .toList();
    }
}
