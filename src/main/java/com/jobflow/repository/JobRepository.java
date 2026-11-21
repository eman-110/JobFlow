package com.jobflow.repository;

import com.jobflow.model.Job;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile({"dev", "test", "prod"})
public class JobRepository implements JobStore {
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

    @Override
    public void add(Job job) {
        jobs.add(job);
    }

    @Override
    public void save(Job job) {
        // Jobs are mutable and the in-memory store retains the same instances.
    }

    @Override
    public List<Job> findAll() {
        return List.copyOf(jobs);
    }

    @Override
    public Page<Job> findPage(Pageable pageable) {
        var order = pageable.getSort().iterator().next();
        Comparator<Job> comparator = switch (order.getProperty()) {
            case "id" -> Comparator.comparingInt(Job::getId);
            case "title" -> Comparator.comparing(Job::getTitle);
            case "location" -> Comparator.comparing(Job::getLocation);
            case "salary" -> Comparator.comparingDouble(Job::getSalary);
            default -> throw new IllegalArgumentException(
                    "Unsupported job sort property: " + order.getProperty());
        };
        if (order.isDescending()) {
            comparator = comparator.reversed();
        }
        List<Job> sorted = jobs.stream().sorted(comparator).toList();
        long offset = pageable.getOffset();
        List<Job> content;
        if (offset >= sorted.size()) {
            content = List.of();
        } else {
            int start = (int) offset;
            int end = Math.min(start + pageable.getPageSize(), sorted.size());
            content = sorted.subList(start, end);
        }
        return new PageImpl<>(content, pageable, sorted.size());
    }

    @Override
    public Optional<Job> findById(int id) {
        return jobs.stream()
                .filter(job -> job.getId() == id)
                .findFirst();
    }

    @Override
    public boolean deleteById(int id) {
        return jobs.removeIf(job -> job.getId() == id);
    }
}
