package com.jobflow.service;

import com.jobflow.exception.JobNotFoundException;
import com.jobflow.model.Job;
import com.jobflow.notification.NotificationService;
import com.jobflow.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceMockitoTest {
    @Mock
    private JobRepository repository;

    @Mock
    private NotificationService notification;

    private JobService service;

    @BeforeEach
    void setUp() {
        service = new JobService(repository, notification, 5, true);
    }

    @Test
    void shouldCreateRetrieveUpdateAndDeleteJob() {
        when(repository.findAll()).thenReturn(List.of());
        Job created = service.createJob("Developer", "Example Co", "London", 100000);

        ArgumentCaptor<Job> addedJob = ArgumentCaptor.forClass(Job.class);
        verify(repository, times(1)).add(addedJob.capture());
        assertSame(created, addedJob.getValue());

        when(repository.findById(created.getId())).thenReturn(Optional.of(created));
        assertSame(created, service.getJobById(created.getId()));

        Job updated = service.updateJob(
                created.getId(), "Senior Developer", "Example Co", "London", 120000
        );
        assertSame(created, updated);
        assertEquals("Senior Developer", updated.getTitle());
        assertEquals(120000, updated.getSalary());

        when(repository.deleteById(created.getId())).thenReturn(true);
        service.deleteJob(created.getId());
        verify(repository).deleteById(created.getId());
    }

    @Test
    void shouldThrowWhenRequestedJobIsMissing() {
        when(repository.findById(404)).thenReturn(Optional.empty());
        when(repository.deleteById(404)).thenReturn(false);

        assertThrows(JobNotFoundException.class, () -> service.getJobById(404));
        assertThrows(JobNotFoundException.class,
                () -> service.updateJob(404, "Developer", "Example Co", "London", 100000));
        assertThrows(JobNotFoundException.class, () -> service.deleteJob(404));
    }

    @Test
    void shouldRejectCreationAtMaximumCapacity() {
        when(repository.findAll()).thenReturn(List.of(
                new Job(1, "Developer", "Example Co", "London", 100000)
        ));
        JobService capacityLimited = new JobService(repository, notification, 1, true);

        assertThrows(IllegalStateException.class,
                () -> capacityLimited.createJob("Analyst", "Example Co", "London", 90000));
        verify(repository, never()).add(any(Job.class));
    }

    @Test
    void shouldSendNotificationWhenEnabled() {
        service.applyForJob(new Job(50, "Developer", "Example Co", "London", 100000));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notification, times(1)).sendNotification(message.capture());
        assertEquals("Application submitted for Developer", message.getValue());
    }

    @Test
    void shouldNotSendNotificationWhenDisabled() {
        JobService notificationsDisabled = new JobService(repository, notification, 5, false);

        notificationsDisabled.applyForJob(new Job(51, "Developer", "Example Co", "London", 100000));

        verify(notification, never()).sendNotification(anyString());
    }
}
