package com.jobflow.controller;

import com.jobflow.api.ApplicationInfo;
import com.jobflow.config.JobFlowProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApplicationInfoController {
    private final JobFlowProperties properties;

    public ApplicationInfoController(JobFlowProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/info")
    public ApplicationInfo getInfo() {
        return new ApplicationInfo(
                properties.getName(),
                properties.getVersion(),
                properties.getEnvironment()
        );
    }
}
