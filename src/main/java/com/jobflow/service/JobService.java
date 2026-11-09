package com.jobflow.service;

import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import com.jobflow.notification.NotificationService;
import com.jobflow.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;

public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final Repository<Job> repository;
    private final NotificationService notification;

    public JobService(Repository<Job> repository, NotificationService notification) {
        this.repository = repository;
        this.notification = notification;
    }

    public void addJob(Job job) {
        repository.add(job);
        logger.info("Job added: {}", job.getTitle());
    }

    public List<Job> getAllJobs() {
        return repository.getAll();
    }

    public List<Job> findJobsByCompany(String company) {
        return repository.getAll()
                .stream()
                .filter(job -> job.getCompany().equalsIgnoreCase(company))
                .toList();
    }

    public List<Job> getHighPayingJobs(double minimumSalary) {
        return repository.getAll()
                .stream()
                .filter(job -> job.getSalary() >= minimumSalary)
                .toList();
    }

    public List<Job> sortBySalaryDescending() {
        return repository.getAll()
                .stream()
                .sorted(Comparator.comparingDouble(Job::getSalary).reversed())
                .toList();
    }

    public List<Job> sortJobsBySalaryAscending() {
        return repository.getAll()
                .stream()
                .sorted(Comparator.comparingDouble(Job::getSalary))
                .toList();
    }

    public double calculateTotalSalary() {
        return repository.getAll()
                .stream()
                .map(Job::getSalary)
                .reduce(0.0, Double::sum);
    }

    public void applyForJob(Job job) {
        if (job.getStatus() == JobStatus.APPLIED) {
            logger.warn("Duplicate application attempt for {}", job.getTitle());
            return;
        }

        job.apply();
        logger.info("Job application submitted: {}", job.getTitle());
        notification.sendNotification("Application submitted for " + job.getTitle());
    }

    public List<Job> searchByTitle(String keyword) {
        return repository.getAll()
                .stream()
                .filter(job -> job.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
