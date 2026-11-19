package com.jobflow.service;

import com.jobflow.exception.JobCapacityExceededException;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import com.jobflow.notification.NotificationService;
import com.jobflow.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final JobRepository repository;
    private final NotificationService notification;
    private final int maximumJobCapacity;
    private final boolean notificationsEnabled;

    public JobService(JobRepository repository,
                      @Qualifier("emailNotification") NotificationService notification) {
        this(repository, notification, 100, true);
    }

    @Autowired
    public JobService(JobRepository repository,
                      @Qualifier("emailNotification") NotificationService notification,
                      @Value("${jobflow.jobs.max-capacity:100}") int maximumJobCapacity,
                      @Value("${jobflow.notifications.enabled:true}") boolean notificationsEnabled) {
        this.repository = repository;
        this.notification = notification;
        this.maximumJobCapacity = maximumJobCapacity;
        this.notificationsEnabled = notificationsEnabled;
    }

    public void addJob(Job job) {
        if (repository.findAll().size() >= maximumJobCapacity) {
            throw new JobCapacityExceededException("Maximum job capacity has been reached.");
        }
        repository.add(job);
        logger.info("Job added: {}", job.getTitle());
    }

    public Job updateJob(int id, Job job) {
        if (id != job.getId()) {
            throw new IllegalArgumentException("The job ID must match the request ID.");
        }
        if (!repository.update(job)) {
            throw new JobNotFoundException("Job not found: " + id);
        }
        return job;
    }

    public void deleteJob(int id) {
        if (!repository.deleteById(id)) {
            throw new JobNotFoundException("Job not found: " + id);
        }
    }

    public List<Job> getAllJobs() {
        return repository.findAll();
    }

    public Optional<Job> findJobById(int id) {
        return repository.findById(id);
    }

    public List<Job> findJobsByCompany(String company) {
        return repository.findAll()
                .stream()
                .filter(job -> job.getCompany().equalsIgnoreCase(company))
                .toList();
    }

    public List<Job> getHighPayingJobs(double minimumSalary) {
        return repository.findAll()
                .stream()
                .filter(job -> job.getSalary() >= minimumSalary)
                .toList();
    }

    public List<Job> sortBySalaryDescending() {
        return repository.findAll()
                .stream()
                .sorted(Comparator.comparingDouble(Job::getSalary).reversed())
                .toList();
    }

    public List<Job> sortJobsBySalaryAscending() {
        return repository.findAll()
                .stream()
                .sorted(Comparator.comparingDouble(Job::getSalary))
                .toList();
    }

    public double calculateTotalSalary() {
        return repository.findAll()
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
        if (notificationsEnabled) {
            notification.sendNotification("Application submitted for " + job.getTitle());
        }
    }

    public List<Job> searchByTitle(String keyword) {
        return repository.findAll()
                .stream()
                .filter(job -> job.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
