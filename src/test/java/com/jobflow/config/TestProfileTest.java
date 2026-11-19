package com.jobflow.config;

import com.jobflow.model.Job;
import com.jobflow.notification.NotificationService;
import com.jobflow.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestProfileTest {
    @Autowired
    private JobFlowProperties properties;

    @Autowired
    private JobService jobService;

    @MockitoBean(name = "emailNotification")
    private NotificationService notification;

    @Test
    void shouldApplyTestCapacityAndDisableNotifications() {
        assertEquals(4, properties.getMaxJobs());
        assertFalse(properties.isNotificationsEnabled());
        assertEquals("test", properties.getEnvironment());

        Job extraJob = jobService.createJob("Tester", "Example Co", "London", 80000);
        assertThrows(IllegalStateException.class,
                () -> jobService.createJob("Analyst", "Example Co", "London", 85000));

        jobService.applyForJob(extraJob);
        verify(notification, never()).sendNotification(anyString());
    }
}
