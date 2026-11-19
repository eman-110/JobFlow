package com.jobflow.repository;

import com.jobflow.model.Job;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JobRepository {
    private final List<Job> jobs = new ArrayList<>();

    public JobRepository() {
        this(List.of(
                new Job(101, "Java Developer", "Tech Solutions", "Mountain View", 150000),
                new Job(102, "Android Developer", "Mobile Labs", "Redmond", 130000),
                new Job(103, "Backend Developer", "Software House", "Seattle", 180000)
        ));
    }

    public JobRepository(List<Job> initialJobs) {
        jobs.addAll(initialJobs);
    }

    public void add(Job job) {
        jobs.add(job);
    }

    public List<Job> findAll() {
        return List.copyOf(jobs);
    }

    public Optional<Job> findById(int id) {
        return jobs.stream()
                .filter(job -> job.getId() == id)
                .findFirst();
    }

    public boolean update(Job updatedJob) {
        for (int index = 0; index < jobs.size(); index++) {
            if (jobs.get(index).getId() == updatedJob.getId()) {
                jobs.set(index, updatedJob);
                return true;
            }
        }
        return false;
    }

    public boolean deleteById(int id) {
        return jobs.removeIf(job -> job.getId() == id);
    }
}
