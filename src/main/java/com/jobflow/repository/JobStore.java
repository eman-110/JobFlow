package com.jobflow.repository;

import com.jobflow.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface JobStore {
    void add(Job job);

    void save(Job job);

    List<Job> findAll();

    Page<Job> findPage(Pageable pageable);

    Optional<Job> findById(int id);

    boolean deleteById(int id);
}
