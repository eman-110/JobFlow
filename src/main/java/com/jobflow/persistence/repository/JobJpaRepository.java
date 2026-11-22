package com.jobflow.persistence.repository;

import com.jobflow.api.JobSummary;
import com.jobflow.persistence.entity.JobEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface JobJpaRepository extends JpaRepository<JobEntity, Integer> {
    List<JobEntity> findByCompany_Id(Integer companyId);

    List<JobEntity> findByTitleContainingIgnoreCase(String title);

    List<JobEntity> findBySalaryGreaterThanEqual(BigDecimal minimumSalary);

    @Query("select job from JobEntity job join job.company company "
            + "where lower(company.name) = lower(:companyName)")
    List<JobEntity> findByCompanyName(@Param("companyName") String companyName);

    @Query("select job from JobEntity job join fetch job.company")
    List<JobEntity> findAllWithCompany();

    @Query(value = "select job from JobEntity job join fetch job.company",
            countQuery = "select count(job) from JobEntity job")
    Page<JobEntity> findAllWithCompany(Pageable pageable);

    @Query("select job from JobEntity job join fetch job.company where job.id = :id")
    Optional<JobEntity> findByIdWithCompany(@Param("id") Integer id);

    @Query("select new com.jobflow.api.JobSummary("
            + "job.id, job.title, company.name, job.location, job.salary, job.status) "
            + "from JobEntity job join job.company company")
    List<JobSummary> findJobSummaries();

    @Query("select job from JobEntity job where job.salary >= :minimumSalary order by job.salary desc")
    List<JobEntity> findHighPayingJobs(@Param("minimumSalary") BigDecimal minimumSalary);

    @Query("select count(job) from JobEntity job where job.company.id = :companyId")
    long countJobsForCompany(@Param("companyId") Integer companyId);
}
