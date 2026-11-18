package com.jobflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevelopmentComponent {
    private static final Logger logger = LoggerFactory.getLogger(DevelopmentComponent.class);

    public DevelopmentComponent() {
        logger.debug("Development-only JobFlow component initialized");
    }

    public String getEnvironmentName() {
        return "development";
    }
}
