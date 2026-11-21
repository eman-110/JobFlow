package com.jobflow.persistence.repository;

import com.jobflow.persistence.entity.CompanyEntity;
import com.jobflow.persistence.entity.JobEntity;
import com.jobflow.model.Job;
import com.jobflow.repository.JobStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("mysql")
@Testcontainers
class JobJpaRepositoryIntegrationTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("jobflow_test")
            .withUsername("jobflow_test")
            .withPassword("test-password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private JobJpaRepository jobs;

    @Autowired
    private CompanyJpaRepository companies;

    @Autowired
    private UserJpaRepository users;

    @Autowired
    private ApplicationJpaRepository applications;

    @Autowired
    private JobStore jobStore;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        jobs.deleteAll();
        applications.deleteAll();
        users.deleteAll();
        companies.deleteAll();
    }

    @Test
    void createsAndReadsJob() {
        JobEntity saved = saveJob("Backend Engineer", "120000.00");

        assertNotNull(saved.getId());
        assertEquals(saved.getId(), jobs.findById(saved.getId()).orElseThrow().getId());
    }

    @Test
    void updatesJob() {
        JobEntity job = saveJob("Backend Engineer", "120000.00");
        job.setTitle("Senior Backend Engineer");

        JobEntity updated = jobs.saveAndFlush(job);

        assertEquals("Senior Backend Engineer", jobs.findById(updated.getId()).orElseThrow().getTitle());
    }

    @Test
    void listsJobs() {
        saveJob("First Engineer", "100000.00");
        saveJob("Second Engineer", "110000.00");

        assertEquals(2, jobs.findAll().size());
    }

    @Test
    void deletesJob() {
        JobEntity job = saveJob("Backend Engineer", "120000.00");

        jobs.deleteById(job.getId());
        jobs.flush();

        assertFalse(jobs.existsById(job.getId()));
    }

    @Test
    void findsJobsByCompanyId() {
        CompanyEntity company = saveCompany();
        saveJob(company, "Backend Engineer", "120000.00");
        saveJob(company, "Frontend Engineer", "110000.00");

        assertEquals(2, jobs.findByCompanyId(company.getId()).size());
    }

    @Test
    void findsTitlesIgnoringCase() {
        saveJob("Senior Backend Engineer", "120000.00");
        saveJob("Frontend Engineer", "110000.00");

        assertEquals(1, jobs.findByTitleContainingIgnoreCase("BACKEND").size());
    }

    @Test
    void findsJobsAtOrAboveMinimumSalary() {
        saveJob("Junior Engineer", "80000.00");
        saveJob("Senior Engineer", "150000.00");

        assertEquals(1, jobs.findBySalaryGreaterThanEqual(new BigDecimal("100000.00")).size());
    }

    @Test
    void executesHighPayingJobsJpqlQueryInSalaryOrder() {
        saveJob("Lower Engineer", "125000.00");
        saveJob("Higher Engineer", "175000.00");
        saveJob("Junior Engineer", "80000.00");

        List<JobEntity> result = jobs.findHighPayingJobs(new BigDecimal("100000.00"));

        assertEquals(2, result.size());
        assertEquals("Higher Engineer", result.getFirst().getTitle());
    }

    @Test
    void executesCompanyCountJpqlQuery() {
        CompanyEntity company = saveCompany();
        saveJob(company, "First Engineer", "100000.00");
        saveJob(company, "Second Engineer", "110000.00");

        assertEquals(2, jobs.countJobsForCompany(company.getId()));
    }

    @Test
    void executesPaginatedQueryWithSorting() {
        saveJob("Lower Engineer", "90000.00");
        saveJob("Higher Engineer", "150000.00");
        saveJob("Middle Engineer", "120000.00");

        Page<JobEntity> result = jobs.findAll(PageRequest.of(
                0, 2, Sort.by(Sort.Direction.DESC, "salary")));

        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("Higher Engineer", result.getContent().getFirst().getTitle());
        assertEquals("Middle Engineer", result.getContent().get(1).getTitle());
    }

    @Test
    void storeUsesGeneratedJobIdsAndReusesCompaniesByName() {
        saveJob("Existing Engineer", "90000.00");
        companies.saveAndFlush(new CompanyEntity("Shared Company", null, "US"));
        Job first = new Job("First New Engineer", "Shared Company", "Remote", 100000);
        Job second = new Job("Second New Engineer", "Shared Company", "Remote", 110000);

        jobStore.add(first);
        jobStore.add(second);

        assertTrue(first.getId() > 1);
        assertTrue(jobs.findById(first.getId()).isPresent());
        assertTrue(second.getId() > first.getId());
        assertTrue(jobs.findById(second.getId()).isPresent());
        assertEquals("Shared Company",
                companies.findByName("shared company").orElseThrow().getName());
        assertEquals(1, companies.findAll().stream()
                .filter(company -> company.getName().equals("Shared Company"))
                .count());
    }

    @Test
    void databaseRejectsNegativeSalary() {
        CompanyEntity company = saveCompany();

        assertThrows(RuntimeException.class, () -> jdbcTemplate.update(
                "INSERT INTO jobs (company_id, title, location, salary, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                company.getId(), "Invalid Engineer", "Remote", new BigDecimal("-1.00"), "SAVED"));
    }

    @Test
    void databaseRejectsMissingCompanyForeignKey() {
        assertThrows(RuntimeException.class, () -> jdbcTemplate.update(
                "INSERT INTO jobs (company_id, title, location, salary, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Integer.MAX_VALUE, "Orphan Engineer", "Remote", new BigDecimal("100000.00"), "SAVED"));
    }

    @Test
    void savedJobIsReadableFromANewPersistenceContext() {
        JobEntity saved = saveJob("Persistent Engineer", "135000.00");
        EntityManager independentEntityManager = entityManagerFactory.createEntityManager();
        try {
            JobEntity retrieved = independentEntityManager.find(JobEntity.class, saved.getId());
            assertNotNull(retrieved);
            assertEquals("Persistent Engineer", retrieved.getTitle());
            assertTrue(independentEntityManager.contains(retrieved));
        } finally {
            independentEntityManager.close();
        }
    }

    private JobEntity saveJob(String title, String salary) {
        return saveJob(saveCompany(), title, salary);
    }

    private JobEntity saveJob(CompanyEntity company, String title, String salary) {
        return jobs.saveAndFlush(new JobEntity(
                null,
                company.getId(),
                title,
                "Remote",
                new BigDecimal(salary)
        ));
    }

    private CompanyEntity saveCompany() {
        return companies.saveAndFlush(new CompanyEntity(
                "Company-" + UUID.randomUUID(),
                null,
                "Unspecified"
        ));
    }
}
