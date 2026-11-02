public class Job {
    private String title;
    private String company;
    private String location;
    private double salary;
    private String status = "Saved";

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

    public String getStatus() {
        return status;
    }

    public void apply() {
        if ("Applied".equals(status)) {
            System.out.println("You have already applied for this job.");
            return;
        }

        status = "Applied";
        System.out.println("Application submitted for " + title + " at " + company + ".");
    }
}
