package com.jobflow.service;

import com.jobflow.model.Job;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.notification.EmailNotification;
import com.jobflow.repository.Repository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobServiceTest {

    @Test
    void shouldAddJob() {
        Repository<Job> repository = new Repository<>();
        JobService service = new JobService(repository, new EmailNotification());

        service.addJob(new Job("Java Developer", "Google", "London", 120000));

        assertEquals(1, service.getAllJobs().size());
    }

    @Test
    void shouldSortJobsBySalaryAscendingWithoutChangingRepositoryOrder() {
        Repository<Job> repository = new Repository<>();
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
    void shouldFindJobByIdAndThrowWhenAbsent() {
        Job job = new Job("Engineer", "Company", "London", 100000);
        java.util.Map<Integer, Job> jobs = java.util.Map.of(7, job);

        assertEquals(job, com.jobflow.app.Main.findJobById(jobs, 7));
        assertThrows(JobNotFoundException.class, () -> com.jobflow.app.Main.findJobById(jobs, 8));
    }
}
