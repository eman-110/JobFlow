package com.jobflow.controller;

import com.jobflow.api.JobResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private static final List<JobResponse> SAMPLE_JOBS = List.of(
            new JobResponse(101, "Java Developer", "Tech Solutions", 150000),
            new JobResponse(102, "Android Developer", "Mobile Labs", 130000),
            new JobResponse(103, "Backend Developer", "Software House", 180000)
    );

    @GetMapping
    public List<JobResponse> getAllJobs() {
        return SAMPLE_JOBS;
    }

    @GetMapping("/{id}")
    public JobResponse getJobById(@PathVariable int id) {
        return new JobResponse(id, "Java Developer", "Tech Solutions", 150000);
    }

    @GetMapping("/search")
    public String searchJobs(@RequestParam String company) {
        return "Searching jobs at: " + company;
    }
}
