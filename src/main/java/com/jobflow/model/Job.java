package com.jobflow.model;

public class Job {
    private String title;
    private String company;
    private String location;
    private double salary;
    private JobStatus status = JobStatus.SAVED;

    public Job(String title, String company, String location, double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative.");
        }

        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
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

    public void apply() {
        if (status == JobStatus.APPLIED) {
            System.out.println("Already applied.");
            return;
        }

        status = JobStatus.APPLIED;
        System.out.println("Application submitted.");
    }
}
