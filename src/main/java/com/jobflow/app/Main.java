package com.jobflow.app;

import com.jobflow.config.AppConfig;
import com.jobflow.exception.JobNotFoundException;
import com.jobflow.model.Job;
import com.jobflow.model.JobSeeker;
import com.jobflow.model.User;
import com.jobflow.notification.EmailNotification;
import com.jobflow.notification.NotificationService;
import com.jobflow.notification.WhatsAppNotification;
import com.jobflow.repository.JobRepository;
import com.jobflow.repository.Repository;
import com.jobflow.service.JobService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Main {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        System.out.println("Application: " + config.getName());
        System.out.println("Version: " + config.getVersion());

        JobRepository repository = new JobRepository(List.of());
        NotificationService notification = new EmailNotification();
        JobService jobService = new JobService(repository, notification);

        Job javaDeveloper = new Job("Java Developer", "Google", "Mountain View", 120000);
        Job softwareEngineer = new Job("Software Engineer", "Microsoft", "Redmond", 115000);
        Job backendDeveloper = new Job("Backend Developer", "Amazon", "Seattle", 140000);
        Job reactNativeDeveloper = new Job("React Native Developer", "Google", "New York", 110000);
        Job androidDeveloper = new Job("Android Developer", "Meta", "Menlo Park", 95000);

        jobService.addJob(javaDeveloper);
        jobService.addJob(softwareEngineer);
        jobService.addJob(backendDeveloper);
        jobService.addJob(reactNativeDeveloper);
        jobService.addJob(androidDeveloper);

        try {
            printJob(new Job("Java Developer", "Google", "London", -50000));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        for (Job job : jobService.getAllJobs()) {
            printJob(job);
        }

        List<Job> jobs = jobService.getAllJobs();
        try {
            jobs.clear();
        } catch (UnsupportedOperationException e) {
            System.out.println("Job list is unmodifiable.");
        }

        Repository<String> skillRepository = new Repository<>();
        skillRepository.add("Java");
        skillRepository.add("Spring Boot");
        skillRepository.add("PostgreSQL");
        System.out.println("Skills:");
        printItems(skillRepository.getAll());

        List<Integer> salaries = List.of(100000, 120000, 140000);
        System.out.println("Total integer salaries: " + calculateTotal(salaries));
        List<Double> bonusAmounts = List.of(1250.5, 2000.0, 749.5);
        System.out.println("Total double amounts: " + calculateTotal(bonusAmounts));

        List<JobSeeker> jobSeekers = new ArrayList<>();
        jobSeekers.add(new JobSeeker("Eman", "eman@example.com"));
        jobSeekers.add(new JobSeeker("Alex", "alex@example.com"));
        System.out.println("Users:");
        displayUsers(jobSeekers);

        System.out.println("Google jobs:");
        for (Job job : jobService.findJobsByCompany("Google")) {
            printJob(job);
        }

        System.out.println("High-paying jobs:");
        for (Job job : jobService.getHighPayingJobs(config.getMinimumSalary())) {
            printJob(job);
        }

        System.out.println("Jobs sorted by salary:");
        for (Job job : jobService.sortBySalaryDescending()) {
            printJob(job);
        }

        System.out.println("Total job salaries: " + jobService.calculateTotalSalary());

        System.out.println("Developer jobs:");
        for (Job job : jobService.searchByTitle("developer")) {
            printJob(job);
        }

        jobService.applyForJob(javaDeveloper);
        System.out.println("Status: " + javaDeveloper.getStatus());
        jobService.applyForJob(javaDeveloper);

        processPendingApplications(jobService);

        JobService whatsAppJobService = new JobService(repository, new WhatsAppNotification());
        Job whatsAppJob = new Job("Java Developer", "Oracle", "Austin", 130000);
        whatsAppJobService.applyForJob(whatsAppJob);

        JobSeeker jobSeeker = new JobSeeker("Eman", "eman@example.com");
        jobSeeker.addSkill("Java");
        jobSeeker.addSkill("React Native");
        jobSeeker.addSkill("Java");
        jobSeeker.addSkill("Spring Boot");
        jobSeeker.displayProfile();

        User user = new JobSeeker("Eman", "eman@example.com");
        user.displayRole();

        Map<Integer, Job> jobsById = new HashMap<>();
        jobsById.put(101, jobs.get(0));
        jobsById.put(102, jobs.get(1));
        jobsById.put(103, jobs.get(2));
        System.out.println("Job 102: " + findJobById(jobsById, 102).getTitle());

        try {
            findJobById(jobsById, 999);
        } catch (JobNotFoundException e) {
            System.out.println(e.getMessage());
        }

        String salaryInput = "120000";
        parseSalary(salaryInput);
        salaryInput = "abc";
        parseSalary(salaryInput);
    }

    public static <T> void printItems(List<T> items) {
        for (T item : items) {
            System.out.println(item);
        }
    }

    public static <T extends Number> double calculateTotal(List<T> numbers) {
        double total = 0;
        for (T number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }

    public static void displayUsers(List<? extends User> users) {
        for (User user : users) {
            System.out.println(user.getName());
        }
    }

    // Expected O(1) lookup with a hash map; worst-case lookup is O(n).
    public static Job findJobById(Map<Integer, Job> jobsById, int id) {
        Job job = jobsById.get(id);
        if (job == null) {
            throw new JobNotFoundException("Job with ID " + id + " was not found.");
        }
        return job;
    }

    private static void parseSalary(String salaryInput) {
        try {
            double salary = Double.parseDouble(salaryInput);
            System.out.println("Salary: " + salary);
        } catch (NumberFormatException e) {
            System.out.println("Invalid salary. Please enter a number.");
        } finally {
            System.out.println("Salary validation finished.");
        }
    }

    private static void processPendingApplications(JobService jobService) {
        Queue<Job> pendingApplications = new ArrayDeque<>();
        pendingApplications.add(new Job("Data Analyst", "Netflix", "Los Gatos", 125000));
        pendingApplications.add(new Job("Product Designer", "Airbnb", "San Francisco", 118000));
        pendingApplications.add(new Job("QA Engineer", "Adobe", "San Jose", 105000));

        while (!pendingApplications.isEmpty()) {
            jobService.applyForJob(pendingApplications.remove());
        }
    }

    private static void printJob(Job job) {
        System.out.println("Title: " + job.getTitle());
        System.out.println("Company: " + job.getCompany());
        System.out.println("Location: " + job.getLocation());
        System.out.println("Salary: " + job.getSalary());
        System.out.println("Status: " + job.getStatus());
        System.out.println();
    }
}
