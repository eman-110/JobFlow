package com.jobflow.config;

import com.jobflow.exception.JobCapacityExceededException;
import com.jobflow.model.Job;
import com.jobflow.notification.NotificationService;
import com.jobflow.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestProfileTest {
    @Autowired
    private JobService jobService;

    @MockitoBean(name = "emailNotification")
    private NotificationService notification;

    @Value("${jobflow.jobs.max-capacity}")
    private int maximumCapacity;

    @Test
    void shouldApplyTestCapacityAndDisableNotifications() {
        assertEquals(4, maximumCapacity);
        Job extraJob = new Job("Tester", "Example Co", "London", 80000);
        jobService.addJob(extraJob);

        assertThrows(JobCapacityExceededException.class,
                () -> jobService.addJob(new Job("Analyst", "Example Co", "London", 85000)));

        jobService.applyForJob(extraJob);
        verify(notification, never()).sendNotification(anyString());
    }
}
