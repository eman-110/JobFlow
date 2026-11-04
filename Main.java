import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        Repository<Job> jobRepository = new Repository<>();
        jobRepository.add(new Job("Java Developer", "Google", "Mountain View", 120000));
        jobRepository.add(new Job("Software Engineer", "Microsoft", "Redmond", 115000));
        jobRepository.add(new Job("Backend Developer", "Amazon", "Seattle", 140000));
        jobRepository.add(new Job("React Native Developer", "Google", "New York", 110000));
        jobRepository.add(new Job("Android Developer", "Meta", "Menlo Park", 95000));
        List<Job> jobs = jobRepository.getAll();

        try {
            printJob(new Job("Java Developer", "Google", "London", -50000));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }

        for (Job job : jobRepository.getAll()) {
            printJob(job);
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

        findJobsByCompany(jobs, "Google");

        System.out.println("High-paying jobs:");
        List<Job> highPayingJobs = getHighPayingJobs(jobs, 115000);
        for (Job job : highPayingJobs) {
            printJob(job);
        }

        System.out.println("Job titles:");
        printItems(getJobTitles(jobs));

        System.out.println("Google job count: " + countJobsByCompany(jobs, "Google"));

        System.out.println("Jobs sorted by salary:");
        for (Job job : sortBySalaryDescending(jobs)) {
            printJob(job);
        }

        Optional<Job> googleJob = findFirstJobByCompany(jobs, "Google");
        googleJob.ifPresent(job -> System.out.println("First Google job: " + job.getTitle()));

        System.out.println("Total job salaries: " + calculateTotalJobSalary(jobs));

        System.out.println("Google job titles by salary:");
        jobs.stream()
                .filter(job -> job.getCompany().equalsIgnoreCase("Google"))
                .sorted(Comparator.comparingDouble(Job::getSalary).reversed())
                .map(Job::getTitle)
                .forEach(System.out::println);

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

        findHighestPayingJobStream(jobs)
                .ifPresent(job -> System.out.println("Highest paying job (stream): " + job.getTitle()));
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

    public static void findJobsByCompany(List<Job> jobs, String company) {
        for (Job job : jobs) {
            if (job.getCompany().equalsIgnoreCase(company)) {
                printJob(job);
            }
        }
    }

    public static List<Job> getHighPayingJobs(List<Job> jobs, double minimumSalary) {
        return jobs.stream()
                .filter(job -> job.getSalary() >= minimumSalary)
                .toList();
    }

    public static List<String> getJobTitles(List<Job> jobs) {
        return jobs.stream()
                .map(Job::getTitle)
                .toList();
    }

    public static long countJobsByCompany(List<Job> jobs, String company) {
        return jobs.stream()
                .filter(job -> job.getCompany().equalsIgnoreCase(company))
                .count();
    }

    public static List<Job> sortBySalaryDescending(List<Job> jobs) {
        return jobs.stream()
                .sorted(Comparator.comparingDouble(Job::getSalary).reversed())
                .toList();
    }

    public static Optional<Job> findFirstJobByCompany(List<Job> jobs, String company) {
        return jobs.stream()
                .filter(job -> job.getCompany().equalsIgnoreCase(company))
                .findFirst();
    }

    public static double calculateTotalJobSalary(List<Job> jobs) {
        return jobs.stream()
                .map(Job::getSalary)
                .reduce(0.0, Double::sum);
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

    public static Optional<Job> findHighestPayingJobStream(List<Job> jobs) {
        return jobs.stream()
                .max(Comparator.comparingDouble(Job::getSalary));
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
