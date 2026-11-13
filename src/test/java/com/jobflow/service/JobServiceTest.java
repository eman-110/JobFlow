package com.jobflow.service;

import com.jobflow.model.Job;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.notification.EmailNotification;
import com.jobflow.repository.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobServiceTest {

    @Test
    void shouldAddJob() {
        JobRepository repository = new JobRepository(List.of());
        JobService service = new JobService(repository, new EmailNotification());

        Job job = new Job("Java Developer", "Google", "London", 120000);
        service.addJob(job);

        assertEquals(1, service.getAllJobs().size());
        assertEquals(Optional.of(job), service.findJobById(job.getId()));
    }

    @Test
    void shouldSortJobsBySalaryAscendingWithoutChangingRepositoryOrder() {
        JobRepository repository = new JobRepository(List.of());
        JobService service = new JobService(repository, new EmailNotification());
        Job higherSalary = new Job("Senior Engineer", "Company A", "London", 120000);
        Job lowerSalary = new Job("Junior Engineer", "Company B", "London", 80000);
        Job middleSalary = new Job("Engineer", "Company C", "London", 100000);
        service.addJob(higherSalary);
        service.addJob(lowerSalary);
        service.addJob(middleSalary);

        assertEquals(List.of(lowerSalary, middleSalary, higherSalary), service.sortJobsBySalaryAscending());
        assertEquals(List.of(higherSalary, lowerSalary, middleSalary), service.getAllJobs());
    }

    @Test
    void shouldFindJobsByCompanyIgnoringCase() {
        Job googleJob = new Job(1, "Developer", "Google", "London", 120000);
        Job microsoftJob = new Job(2, "Engineer", "Microsoft", "London", 140000);
        JobRepository repository = new JobRepository(List.of(googleJob, microsoftJob));
        JobService service = new JobService(repository, new EmailNotification());

        assertEquals(List.of(googleJob), service.findJobsByCompany("gOoGlE"));
    }

    @Test
    void shouldFilterJobsByMinimumSalary() {
        Job lowerSalary = new Job(1, "Developer", "Google", "London", 90000);
        Job matchingSalary = new Job(2, "Engineer", "Microsoft", "London", 120000);
        JobRepository repository = new JobRepository(List.of(lowerSalary, matchingSalary));
        JobService service = new JobService(repository, new EmailNotification());

        assertEquals(List.of(matchingSalary), service.getHighPayingJobs(120000));
    }

    @Test
    void shouldReturnEmptyWhenJobIdIsNotFound() {
        JobService service = new JobService(new JobRepository(List.of()), new EmailNotification());

        assertTrue(service.findJobById(999).isEmpty());
    }

    @Test
    void shouldFindJobByIdAndThrowWhenAbsent() {
        Job job = new Job("Engineer", "Company", "London", 100000);
        java.util.Map<Integer, Job> jobs = java.util.Map.of(7, job);

        assertEquals(job, com.jobflow.app.Main.findJobById(jobs, 7));
        assertThrows(JobNotFoundException.class, () -> com.jobflow.app.Main.findJobById(jobs, 8));
    }
}
