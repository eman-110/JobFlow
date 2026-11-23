package com.jobflow.controller;

import com.jobflow.api.ApplicationResponse;
import com.jobflow.persistence.entity.ApplicationEntity;
import com.jobflow.service.JobApplicationService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/applications")
@Profile("mysql")
public class JobApplicationController {
    private final JobApplicationService applications;

    public JobApplicationController(JobApplicationService applications) {
        this.applications = applications;
    }

    @PostMapping("/{jobId}")
    public ResponseEntity<ApplicationResponse> apply(
            @PathVariable Integer jobId,
            Principal principal) {
        ApplicationEntity application = applications.applyForAuthenticatedUser(
                principal.getName(), jobId);
        return ResponseEntity.created(URI.create("/api/applications/" + application.getId()))
                .body(ApplicationResponse.from(application));
    }
}
