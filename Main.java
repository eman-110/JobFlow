import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        List<Job> jobs = new ArrayList<>();
        jobs.add(new Job("Java Developer", "Google", "Mountain View", 120000));
        jobs.add(new Job("Software Engineer", "Microsoft", "Redmond", 115000));
        jobs.add(new Job("Backend Developer", "Amazon", "Seattle", 140000));
        jobs.add(new Job("React Native Developer", "Google", "New York", 110000));
        jobs.add(new Job("Android Developer", "Meta", "Menlo Park", 95000));

        try {
            printJob(new Job("Java Developer", "Google", "London", -50000));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        for (Job job : jobs) {
            printJob(job);
        }

        findJobsByCompany(jobs, "Google");

        jobs.get(0).apply();
        System.out.println("Status: " + jobs.get(0).getStatus());
        jobs.get(0).apply();

        NotificationService notification = new EmailNotification();
        notification.sendNotification("Application submitted successfully");

        notification = new SmsNotification();
        notification.sendNotification("Application submitted successfully");

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

        Job highestPayingJob = findHighestPayingJob(jobs);
        if (highestPayingJob != null) {
            System.out.println("Highest paying job:");
            printJob(highestPayingJob);
        }
    }

    public static void findJobsByCompany(List<Job> jobs, String company) {
        for (Job job : jobs) {
            if (job.getCompany().equalsIgnoreCase(company)) {
                printJob(job);
            }
        }
    }

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

    public static Job findHighestPayingJob(List<Job> jobs) {
        if (jobs.isEmpty()) {
            return null;
        }

        Job highestPayingJob = jobs.get(0);
        for (Job job : jobs) {
            if (job.getSalary() > highestPayingJob.getSalary()) {
                highestPayingJob = job;
            }
        }
        return highestPayingJob;
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
