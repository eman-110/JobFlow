package com.jobflow.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Job {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int id;
    private String title;
    private String company;
    private String location;
    private double salary;
    private JobStatus status = JobStatus.SAVED;

    public Job(String title, String company, String location, double salary) {
        this(NEXT_ID.getAndIncrement(), title, company, location, salary);
    }

    public Job(int id, String title, String company, String location, double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }

        this.id = id;
        NEXT_ID.accumulateAndGet(id + 1, Math::max);
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public double getSalary() {
        return salary;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void updateDetails(String title, String company, String location, double salary) {
        validateSalary(salary);
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
    }

    public void updateSalary(double salary) {
        validateSalary(salary);
        this.salary = salary;
    }

    public void updateStatus(JobStatus nextStatus) {
        if (nextStatus == null || !status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "Cannot change job status from " + status + " to " + nextStatus + "."
            );
        }
        status = nextStatus;
    }

    public void apply() {
        if (status == JobStatus.APPLIED) {
            System.out.println("Already applied.");
            return;
        }

        status = JobStatus.APPLIED;
        System.out.println("Application submitted.");
    }

    private static void validateSalary(double salary) {
        if (!Double.isFinite(salary) || salary < 0) {
            throw new IllegalArgumentException("Salary must be a non-negative finite number.");
        }
    }
}
