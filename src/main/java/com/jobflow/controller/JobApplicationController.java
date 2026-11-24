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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.net.URI;

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
            @AuthenticationPrincipal Jwt jwt) {
        Integer userId;
        try {
            if (jwt == null || jwt.getSubject() == null) {
                throw new NumberFormatException("Missing authenticated subject.");
            }
            userId = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Authenticated subject is invalid.");
        }
        ApplicationEntity application = applications.applyForAuthenticatedUserId(userId, jobId);
        return ResponseEntity.created(URI.create("/api/applications/" + application.getId()))
                .body(ApplicationResponse.from(application));
    }
}
