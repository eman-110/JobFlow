import java.util.HashSet;
import java.util.Set;

public class JobSeeker extends User {
    private final Set<String> skills = new HashSet<>();

    public JobSeeker(String name, String email) {
        super(name, email);
    }

    public void addSkill(String skill) {
        skills.add(skill);
    }

    public void displayProfile() {
        System.out.println("Name: " + getName());
        System.out.println("Email: " + getEmail());
        displaySkills();
    }

    public void displaySkills() {
        System.out.println("Skills: " + skills);
    }

    @Override
    public void displayRole() {
        System.out.println("Job Seeker");
    }
}
