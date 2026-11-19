package com.jobflow.service;

import com.jobflow.exception.JobCapacityExceededException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        service = new JobService(repository, notification, 2, true);
    }

    @Test
    void shouldCreateRetrieveUpdateAndDeleteJobs() {
        Job created = new Job(41, "Developer", "Example Co", "London", 100000);
        when(repository.findAll()).thenReturn(List.of());
        service.addJob(created);

        ArgumentCaptor<Job> addedJob = ArgumentCaptor.forClass(Job.class);
        verify(repository, times(1)).add(addedJob.capture());
        assertEquals(created, addedJob.getValue());

        when(repository.findById(41)).thenReturn(Optional.of(created));
        assertEquals(Optional.of(created), service.findJobById(41));

        Job updated = new Job(41, "Senior Developer", "Example Co", "London", 120000);
        when(repository.update(updated)).thenReturn(true);
        assertEquals(updated, service.updateJob(41, updated));

        when(repository.deleteById(41)).thenReturn(true);
        service.deleteJob(41);
        verify(repository).deleteById(41);
    }

    @Test
    void shouldThrowWhenUpdatingOrDeletingMissingJobs() {
        Job missing = new Job(99, "Developer", "Example Co", "London", 100000);
        when(repository.update(missing)).thenReturn(false);
        when(repository.deleteById(99)).thenReturn(false);

        assertThrows(JobNotFoundException.class, () -> service.updateJob(99, missing));
        assertThrows(JobNotFoundException.class, () -> service.deleteJob(99));
        assertTrue(service.findJobById(99).isEmpty());
    }

    @Test
    void shouldRejectCreationWhenAtMaximumCapacity() {
        when(repository.findAll()).thenReturn(List.of(
                new Job(1, "Developer", "One", "London", 90000),
                new Job(2, "Engineer", "Two", "London", 100000)
        ));

        assertThrows(JobCapacityExceededException.class,
                () -> service.addJob(new Job(3, "Designer", "Three", "London", 80000)));
        verify(repository, never()).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSendNotificationWhenEnabled() {
        Job job = new Job(50, "Developer", "Example Co", "London", 100000);
        service.applyForJob(job);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notification, times(1)).sendNotification(message.capture());
        assertEquals("Application submitted for Developer", message.getValue());
    }

    @Test
    void shouldNotSendNotificationWhenDisabled() {
        JobService notificationsDisabled = new JobService(repository, notification, 2, false);

        notificationsDisabled.applyForJob(new Job(51, "Developer", "Example Co", "London", 100000));

        verify(notification, never()).sendNotification(org.mockito.ArgumentMatchers.anyString());
    }
}
