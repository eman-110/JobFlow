package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface JobJpaRepository extends JpaRepository<JobEntity, Integer> {
    List<JobEntity> findByCompanyId(Integer companyId);

    List<JobEntity> findByTitleContainingIgnoreCase(String title);

    List<JobEntity> findBySalaryGreaterThanEqual(BigDecimal minimumSalary);

    @Query("select job from JobEntity job where job.salary >= :minimumSalary order by job.salary desc")
    List<JobEntity> findHighPayingJobs(@Param("minimumSalary") BigDecimal minimumSalary);

    @Query("select count(job) from JobEntity job where job.companyId = :companyId")
    long countJobsForCompany(@Param("companyId") Integer companyId);
}
