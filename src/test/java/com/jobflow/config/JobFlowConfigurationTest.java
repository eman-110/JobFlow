package com.jobflow.config;

import com.jobflow.JobFlowApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = JobFlowApplication.class,
        properties = {
                "jobflow.name=Test JobFlow",
                "jobflow.version=9.9",
                "jobflow.environment=testing",
                "jobflow.notifications.enabled=false",
                "jobflow.max-jobs=7"
        })
class JobFlowConfigurationTest {
    @Autowired
    private JobFlowProperties properties;

    @Autowired
    private Environment environment;

    @Test
    void shouldBindAndValidateConfigurationProperties() {
        assertEquals("Test JobFlow", properties.getName());
        assertEquals("9.9", properties.getVersion());
        assertEquals("testing", properties.getEnvironment());
        assertEquals(7, properties.getMaxJobs());
        assertTrue(!properties.isNotificationsEnabled());
    }

    @Test
    void shouldLoadAProfileFile() {
        assertTrue(environment.getActiveProfiles().length == 0
                || java.util.Arrays.asList(environment.getActiveProfiles()).contains("dev"));
    }
}
