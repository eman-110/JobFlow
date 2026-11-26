package com.jobflow.service;

import com.jobflow.config.JobFlowProperties;
import com.jobflow.model.Job;
import com.jobflow.model.JobStatus;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.notification.NotificationService;
import com.jobflow.repository.JobStore;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    private final JobStore repository;
    private final NotificationService notification;
    private final int maxJobs;
    private final boolean notificationsEnabled;

    public JobService(JobStore repository,
                      @Qualifier("emailNotification") NotificationService notification) {
        this(repository, notification, 100, true);
    }

    public JobService(JobStore repository,
                      @Qualifier("emailNotification") NotificationService notification,
                      int maxJobs) {
        this(repository, notification, maxJobs, true);
    }

    public JobService(JobStore repository,
                      @Qualifier("emailNotification") NotificationService notification,
                      int maxJobs,
                      boolean notificationsEnabled) {
        this.repository = repository;
        this.notification = notification;
        this.maxJobs = maxJobs;
        this.notificationsEnabled = notificationsEnabled;
    }

    @Autowired
    public JobService(JobStore repository,
                      @Qualifier("emailNotification") NotificationService notification,
                      JobFlowProperties properties) {
        this(repository, notification, properties.getMaxJobs(),
                properties.isNotificationsEnabled());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#p0.id")
    })
    public void addJob(Job job) {
        ensureCapacity();
        logger.debug("Adding job with ID {}", job.getId());
        repository.add(job);
        logger.info("Job added: {}", job.getTitle());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#result.id")
    })
    public Job createJob(String title, String company, String location, double salary) {
        validateRequiredText(title, "title");
        validateRequiredText(company, "company");
        validateRequiredText(location, "location");
        ensureCapacity();
        logger.debug("Creating job for company {}", company);
        Job job = new Job(title, company, location, salary);
        repository.add(job);
        logger.info("Job created: {}", job.getTitle());
        return job;
    }

    @Cacheable(cacheNames = "jobs", key = "'all'")
    public List<Job> getAllJobs() {
        return repository.findAll();
    }

    public Page<Job> getJobsPage(int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        if (!List.of("id", "title", "location", "salary").contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported job sort property: " + sortBy);
        }
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return repository.findPage(pageable);
    }

    public Optional<Job> findJobById(int id) {
        return repository.findById(id);
    }

    @Cacheable(cacheNames = "jobDetails", key = "#p0")
    public Job getJobById(int id) {
        return findJobById(id)
                .orElseThrow(() -> new JobNotFoundException("Job with ID " + id + " was not found."));
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#p0")
    })
    public Job updateJob(int id, String title, String company, String location, double salary) {
        validateRequiredText(title, "title");
        validateRequiredText(company, "company");
        validateRequiredText(location, "location");
        Job job = getJobById(id);
        job.updateDetails(title, company, location, salary);
        repository.save(job);
        return job;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#p0")
    })
    public Job updateSalary(int id, double salary) {
        Job job = getJobById(id);
        job.updateSalary(salary);
        repository.save(job);
        return job;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#p0")
    })
    public Job updateStatus(int id, JobStatus status) {
        Job job = getJobById(id);
        job.updateStatus(status);
        repository.save(job);
        return job;
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "jobs", allEntries = true),
            @CacheEvict(cacheNames = "jobDetails", key = "#p0")
    })
    public void deleteJob(int id) {
        if (!repository.deleteById(id)) {
            logger.warn("Delete failed: job with ID {} was not found", id);
            throw new JobNotFoundException("Job with ID " + id + " was not found.");
        }
        logger.info("Deleted job with ID {}", id);
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
        } else {
            logger.debug("Notifications disabled; skipped application notification");
        }
    }

    public List<Job> searchByTitle(String keyword) {
        return repository.findAll()
                .stream()
                .filter(job -> job.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    private static void validateRequiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
    }

    private void ensureCapacity() {
        if (repository.findAll().size() >= maxJobs) {
            logger.warn("Job capacity reached: maximum is {}", maxJobs);
            throw new IllegalStateException("Maximum job capacity of " + maxJobs + " reached.");
        }
    }
}
