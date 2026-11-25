package com.jobflow.persistence.repository;

import com.jobflow.api.JobSummary;
import com.jobflow.persistence.entity.ApplicationEntity;
import com.jobflow.persistence.entity.CompanyEntity;
import com.jobflow.persistence.entity.JobEntity;
import com.jobflow.persistence.entity.UserEntity;
import com.jobflow.persistence.entity.UserRole;
import com.jobflow.persistence.entity.ExternalIdentityEntity;
import com.jobflow.persistence.repository.ExternalIdentityJpaRepository;
import com.jobflow.persistence.repository.OAuthLoginExchangeJpaRepository;
import com.jobflow.persistence.repository.OAuthLinkIntentJpaRepository;
import com.jobflow.model.Job;
import com.jobflow.repository.JobStore;
import com.jobflow.service.JobApplicationService;
import com.jobflow.service.RefreshTokenService;
import com.jobflow.service.RefreshTokenService.RefreshTokenException;
import com.jobflow.service.OAuthLoginExchangeService;
import com.jobflow.service.OAuthLoginExchangeService.ExchangeException;
import com.jobflow.service.OAuthLinkIntentService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
@Testcontainers
@org.springframework.security.test.context.support.WithMockUser(roles = "ADMIN")
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
        registry.add("jobflow.jwt.secret",
                () -> "4Bk/UD43/8ySHPhA+W3IHDYXk/Ce0oEg2Vm2rTjAlBU=");
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
    private RefreshTokenService refreshTokens;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private ExternalIdentityJpaRepository externalIdentities;

    @Autowired
    private OAuthLoginExchangeJpaRepository oauthLoginExchanges;

    @Autowired
    private OAuthLinkIntentJpaRepository oauthLinkIntents;

    @Autowired
    private OAuthLoginExchangeService oauthExchanges;

    @Autowired
    private OAuthLinkIntentService oauthLinkService;

    @Autowired
    private JobStore jobStore;

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearDatabase() {
        oauthLinkIntents.deleteAll();
        oauthLoginExchanges.deleteAll();
        externalIdentities.deleteAll();
        refreshTokenRepository.deleteAll();
        applications.deleteAll();
        jobs.deleteAll();
        users.deleteAll();
        companies.deleteAll();
    }

    @Test
    void persistsAndResolvesExternalIdentityByIssuerAndSubject() {
        UserEntity user = saveUser();
        ExternalIdentityEntity saved = externalIdentities.saveAndFlush(new ExternalIdentityEntity(
                user, "https://accounts.google.com", "stable-google-subject"));

        assertNotNull(saved.getId());
        assertEquals(user.getId(), externalIdentities.findByIssuerAndSubject(
                "https://accounts.google.com", "stable-google-subject").orElseThrow().getUser().getId());
    }

    @Test
    void uniqueIssuerAndSubjectCannotBeLinkedToTwoLocalAccounts() {
        UserEntity first = saveUser();
        UserEntity second = saveUser();
        externalIdentities.saveAndFlush(new ExternalIdentityEntity(
                first, "https://accounts.google.com", "unique-subject"));

        assertThrows(DataIntegrityViolationException.class, () -> externalIdentities.saveAndFlush(
                new ExternalIdentityEntity(second, "https://accounts.google.com", "unique-subject")));
    }

    @Test
    void deletingLocalAccountCascadesItsExternalIdentity() {
        UserEntity user = saveUser();
        externalIdentities.saveAndFlush(new ExternalIdentityEntity(
                user, "https://accounts.google.com", "delete-subject"));

        users.deleteById(user.getId());
        users.flush();

        assertTrue(externalIdentities.findByIssuerAndSubject(
                "https://accounts.google.com", "delete-subject").isEmpty());
    }

    @Test
    void oauthExchangeStoresOnlyHashAndEnforcesSingleUse() {
        UserEntity user = saveUser();
        String verifier = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._-";
        String challenge = pkceChallenge(verifier);
        String code = oauthExchanges.issue(user, "integration-client", challenge);

        var stored = oauthLoginExchanges.findAll().getFirst();
        assertNotEquals(code, stored.getCredentialHash());
        assertEquals(64, stored.getCredentialHash().length());
        assertEquals(user.getId(), oauthExchanges.consume(code, "integration-client", verifier).getId());
        assertThrows(ExchangeException.class,
                () -> oauthExchanges.consume(code, "integration-client", verifier));
    }

    @Test
    void concurrentOauthExchangeRequestsConsumeCredentialAtMostOnce() throws Exception {
        UserEntity user = saveUser();
        String verifier = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._-";
        String code = oauthExchanges.issue(user, "integration-client", pkceChallenge(verifier));
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> consumeExchange(code, verifier, ready, start));
            var second = executor.submit(() -> consumeExchange(code, verifier, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void oauthLinkIntentIsHashedAndConsumedOnlyOnce() {
        UserEntity user = saveUser();
        String verifier = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._-";
        String intent = oauthLinkService.issue(user, "integration-client", pkceChallenge(verifier));

        var stored = oauthLinkIntents.findAll().getFirst();
        assertNotEquals(intent, stored.getIntentHash());
        assertEquals(64, stored.getIntentHash().length());
        assertEquals(user.getId(), oauthLinkService.consume(
                intent, "integration-client", pkceChallenge(verifier)));
        assertThrows(ExchangeException.class, () -> oauthLinkService.consume(
                intent, "integration-client", pkceChallenge(verifier)));
    }

    @Test
    void oauthLinkIntentRejectsWrongClientOrChallengeWithoutConsumingIt() {
        UserEntity user = saveUser();
        String verifier = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ._-";
        String intent = oauthLinkService.issue(user, "integration-client", pkceChallenge(verifier));

        assertThrows(ExchangeException.class, () -> oauthLinkService.consume(
                intent, "wrong-client", pkceChallenge(verifier)));
        assertThrows(ExchangeException.class, () -> oauthLinkService.consume(
                intent, "integration-client", pkceChallenge(verifier + "wrong")));
        assertEquals(user.getId(), oauthLinkService.consume(
                intent, "integration-client", pkceChallenge(verifier)));
    }

    private int consumeExchange(String code, String verifier, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Start signal timed out.");
        try {
            oauthExchanges.consume(code, "integration-client", verifier);
            return 1;
        } catch (ExchangeException rejected) {
            return 0;
        }
    }

    private static String pkceChallenge(String verifier) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
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

        assertEquals(2, jobs.findByCompany_Id(company.getId()).size());
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

        Page<Job> storePage = jobStore.findPage(PageRequest.of(
                0, 2, Sort.by(Sort.Direction.DESC, "salary")));
        assertEquals(3, storePage.getTotalElements());
        assertEquals("Higher Engineer", storePage.getContent().getFirst().getTitle());
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

    @Test
    @Transactional
    void companyAndJobRelationshipsAreBidirectionalAfterReload() {
        CompanyEntity company = saveCompany();
        JobEntity saved = saveJob(company, "Relationship Engineer", "125000.00");
        entityManager.flush();
        entityManager.clear();

        CompanyEntity reloadedCompany = companies.findById(company.getId()).orElseThrow();
        JobEntity reloadedJob = jobs.findByIdWithCompany(saved.getId()).orElseThrow();

        assertEquals(1, reloadedCompany.getJobs().size());
        assertEquals(saved.getId(), reloadedCompany.getJobs().getFirst().getId());
        assertEquals(company.getId(), reloadedJob.getCompany().getId());
    }

    @Test
    void jobPersistsItsCompanyAsAnEntityReference() {
        CompanyEntity company = saveCompany();
        JobEntity saved = saveJob(company, "Company Reference Engineer", "130000.00");
        entityManagerFactory.getCache().evictAll();

        JobEntity reloaded = jobs.findByIdWithCompany(saved.getId()).orElseThrow();

        assertEquals(company.getName(), reloaded.getCompany().getName());
        assertEquals(company.getId(), reloaded.getCompanyId());
    }

    @Test
    @Transactional
    void userAndApplicationRelationshipsAreBidirectionalAfterReload() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("User Application Engineer", "125000.00");
        ApplicationEntity saved = applicationService.apply(user.getId(), job.getId());
        entityManager.flush();
        entityManager.clear();

        UserEntity reloadedUser = users.findById(user.getId()).orElseThrow();
        ApplicationEntity reloadedApplication = applications.findById(saved.getId()).orElseThrow();

        assertEquals(1, reloadedUser.getApplications().size());
        assertEquals(saved.getId(), reloadedUser.getApplications().getFirst().getId());
        assertEquals(user.getId(), reloadedApplication.getUser().getId());
    }

    @Test
    void applicationJoinFetchLoadsItsUserJobAndCompany() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Fetched Application Engineer", "135000.00");
        applicationService.apply(user.getId(), job.getId());
        entityManagerFactory.getCache().evictAll();

        ApplicationEntity result = applications
                .findAllWithUserAndJobByUserEmail(user.getEmail())
                .getFirst();

        assertEquals(user.getId(), result.getUser().getId());
        assertEquals(job.getId(), result.getJob().getId());
        assertEquals(job.getCompanyId(), result.getJob().getCompany().getId());
    }

    @Test
    @Transactional
    void jobAndApplicationRelationshipsAreBidirectionalAfterReload() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Job Application Engineer", "140000.00");
        ApplicationEntity saved = applicationService.apply(user.getId(), job.getId());
        entityManager.flush();
        entityManager.clear();

        JobEntity reloadedJob = jobs.findById(job.getId()).orElseThrow();
        ApplicationEntity reloadedApplication = applications.findById(saved.getId()).orElseThrow();

        assertEquals(1, reloadedJob.getApplications().size());
        assertEquals(saved.getId(), reloadedJob.getApplications().getFirst().getId());
        assertEquals(job.getId(), reloadedApplication.getJob().getId());
    }

    @Test
    @Transactional
    void applicationReferencesThePersistedJobEntity() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Persisted Job Reference Engineer", "145000.00");
        ApplicationEntity saved = applicationService.apply(user.getId(), job.getId());
        entityManager.flush();
        entityManager.clear();

        ApplicationEntity reloaded = applications.findById(saved.getId()).orElseThrow();

        assertEquals(job.getId(), reloaded.getJob().getId());
        assertEquals(job.getTitle(), reloaded.getJob().getTitle());
    }

    @Test
    void joinsCompanyToFindJobsByCompanyName() {
        CompanyEntity company = saveCompany();
        saveJob(company, "Joined Company Engineer", "150000.00");
        saveJob("Other Company Engineer", "140000.00");

        List<JobEntity> results = jobs.findByCompanyName(company.getName());

        assertEquals(1, results.size());
        assertEquals("Joined Company Engineer", results.getFirst().getTitle());
    }

    @Test
    @Transactional
    void joinFetchAvoidsNPlusOneCompanyQueries() {
        CompanyEntity firstCompany = saveCompany();
        CompanyEntity secondCompany = saveCompany();
        saveJob(firstCompany, "N Plus One First", "100000.00");
        saveJob(secondCompany, "N Plus One Second", "110000.00");
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        entityManager.clear();
        statistics.clear();
        List<String> companyNames = jobs.findAll().stream()
                .map(job -> job.getCompany().getName())
                .toList();
        long nPlusOneStatementCount = statistics.getPrepareStatementCount();

        entityManager.clear();
        statistics.clear();
        List<JobEntity> fetchedJobs = jobs.findAllWithCompany();
        List<String> fetchedCompanyNames = fetchedJobs.stream()
                .map(job -> job.getCompany().getName())
                .toList();
        long joinFetchStatementCount = statistics.getPrepareStatementCount();

        assertEquals(2, companyNames.size());
        assertEquals(companyNames, fetchedCompanyNames);
        assertTrue(nPlusOneStatementCount >= 3);
        assertEquals(1, joinFetchStatementCount);
    }

    @Test
    void projectsLightweightJobSummariesWithoutEntityGraphs() {
        CompanyEntity company = saveCompany();
        saveJob(company, "Projected Engineer", "155000.00");

        List<JobSummary> summaries = jobs.findJobSummaries();

        assertEquals(1, summaries.size());
        JobSummary summary = summaries.getFirst();
        assertEquals("Projected Engineer", summary.title());
        assertEquals(company.getName(), summary.companyName());
        assertEquals(new BigDecimal("155000.00"), summary.salary());
    }

    @Test
    void applicationServiceRejectsDuplicatesAndDatabaseKeepsUniqueConstraint() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Unique Application Engineer", "160000.00");
        applicationService.apply(user.getId(), job.getId());

        assertThrows(IllegalStateException.class,
                () -> applicationService.apply(user.getId(), job.getId()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO applications (user_id, job_id, status, applied_at) "
                        + "VALUES (?, ?, 'APPLIED', CURRENT_TIMESTAMP)",
                user.getId(), job.getId()));
        assertEquals(1, applications.count());
    }

    @Test
    void applicationServiceRejectsUnknownUsersAndJobs() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Application Validation Engineer", "162000.00");

        assertThrows(IllegalArgumentException.class,
                () -> applicationService.apply(Integer.MAX_VALUE, job.getId()));
        assertThrows(IllegalArgumentException.class,
                () -> applicationService.apply(user.getId(), Integer.MAX_VALUE));
        assertEquals(0, applications.count());
    }

    @Test
    void authenticatedUserSubmitsApplicationForTheirOwnAccount() throws Exception {
        UserEntity user = saveAuthenticatedUser("applicant@example.test");
        JobEntity job = saveJob("Authenticated Applicant Engineer", "175000.00");

        mockMvc.perform(post("/api/applications/{jobId}", job.getId())
                        .with(jwt().jwt(j -> j.subject(user.getId().toString())
                                .claim("role", "USER")
                                .claim("token_type", "access"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.jobId").value(job.getId()))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        assertTrue(applications.existsByUser_IdAndJob_Id(user.getId(), job.getId()));
    }

    @Test
    void submittedApplicationUsesAuthenticatedIdentityInsteadOfRequestedUserId() throws Exception {
        UserEntity authenticatedUser = saveAuthenticatedUser("authenticated@example.test");
        UserEntity otherUser = saveAuthenticatedUser("other@example.test");
        JobEntity job = saveJob("Identity-bound Applicant Engineer", "180000.00");

        SecurityContext originalContext = SecurityContextHolder.getContext();
        try {
            SecurityContext mismatchedContext = SecurityContextHolder.createEmptyContext();
            Jwt authenticatedJwt = Jwt.withTokenValue("service-boundary-test")
                    .header("alg", "none")
                    .subject(authenticatedUser.getId().toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(60))
                    .build();
            mismatchedContext.setAuthentication(new JwtAuthenticationToken(
                    authenticatedJwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            SecurityContextHolder.setContext(mismatchedContext);

            assertThrows(AccessDeniedException.class,
                    () -> applicationService.applyForAuthenticatedUserId(otherUser.getId(), job.getId()));
        } finally {
            SecurityContextHolder.setContext(originalContext);
        }

        mockMvc.perform(post("/api/applications/{jobId}", job.getId())
                        .param("userId", otherUser.getId().toString())
                        .with(jwt().jwt(j -> j.subject(authenticatedUser.getId().toString())
                                .claim("role", "USER")
                                .claim("token_type", "access"))
                                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                        
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(otherUser.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        assertTrue(applications.existsByUser_IdAndJob_Id(authenticatedUser.getId(), job.getId()));
        assertFalse(applications.existsByUser_IdAndJob_Id(otherUser.getId(), job.getId()));
    }

    @Test
    void reusedRefreshTokenRevokesItsEntireFamilyInTheDatabase() {
        UserEntity user = saveUser();
        String tokenA = refreshTokens.create(user);
        RefreshTokenService.RotatedUser rotation = refreshTokens.rotate(tokenA);
        String tokenB = rotation.replacement();

        assertThrows(RefreshTokenException.class, () -> refreshTokens.rotate(tokenA));
        assertThrows(RefreshTokenException.class, () -> refreshTokens.rotate(tokenB));

        List<com.jobflow.persistence.entity.RefreshTokenEntity> family = refreshTokenRepository.findAll();
        assertEquals(2, family.size());
        assertTrue(family.stream().allMatch(token -> token.getRevokedAt() != null));
    }

    @Test
    void concurrentRefreshRequestsAllowOnlyOneRotationAndRevokeTheFamily() throws Exception {
        UserEntity user = saveUser();
        String tokenA = refreshTokens.create(user);
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> rotateConcurrently(tokenA, ready, start));
            var second = executor.submit(() -> rotateConcurrently(tokenA, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<String> replacements = java.util.stream.Stream.of(
                            first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))
                    .filter(java.util.Objects::nonNull).toList();
            assertEquals(1, replacements.size());
            assertThrows(RefreshTokenException.class, () -> refreshTokens.rotate(replacements.getFirst()));
            assertTrue(refreshTokenRepository.findAll().stream()
                    .allMatch(token -> token.getRevokedAt() != null));
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private String rotateConcurrently(String credential, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent refresh test did not start.");
        }
        try {
            return refreshTokens.rotate(credential).replacement();
        } catch (RefreshTokenException rejected) {
            return null;
        }
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "RECRUITER")
    void applicationServiceMethodSecurityRejectsRecruiters() {
        UserEntity user = saveAuthenticatedUser("method-security@example.test");
        JobEntity job = saveJob("Method Security Engineer", "185000.00");

        assertThrows(AccessDeniedException.class,
                () -> applicationService.applyForAuthenticatedUserId(user.getId(), job.getId()));
        assertFalse(applications.existsByUser_IdAndJob_Id(user.getId(), job.getId()));
    }

    @Test
    void applicationIsRolledBackWhenAnEnclosingWorkflowFails() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Rollback Workflow Engineer", "165000.00");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            applicationService.apply(user.getId(), job.getId());
            throw new IllegalStateException("A later workflow step failed.");
        }));

        assertFalse(applications.existsByUser_IdAndJob_Id(user.getId(), job.getId()));
    }

    @Test
    void applicationIsRolledBackWhenALaterDatabaseConstraintFails() {
        UserEntity user = saveUser();
        JobEntity job = saveJob("Rollback Constraint Engineer", "170000.00");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(DataIntegrityViolationException.class,
                () -> transaction.executeWithoutResult(status -> {
                    applicationService.apply(user.getId(), job.getId());
                    jdbcTemplate.update(
                            "INSERT INTO applications (user_id, job_id, status, applied_at) "
                                    + "VALUES (?, ?, 'APPLIED', CURRENT_TIMESTAMP)",
                            user.getId(), job.getId());
                }));

        assertEquals(0, applications.count());
    }

    private JobEntity saveJob(String title, String salary) {
        return saveJob(saveCompany(), title, salary);
    }

    private JobEntity saveJob(CompanyEntity company, String title, String salary) {
        return jobs.saveAndFlush(new JobEntity(
                null,
                company,
                title,
                "Remote",
                new BigDecimal(salary)
        ));
    }

    private UserEntity saveUser() {
        String uniqueEmail = "user-" + UUID.randomUUID() + "@example.test";
        return users.saveAndFlush(new UserEntity("Integration User", uniqueEmail));
    }

    private UserEntity saveAuthenticatedUser(String email) {
        UserEntity user = new UserEntity("Security Integration User", email);
        user.setPasswordHash(passwordEncoder.encode("user-password"));
        user.setRole(UserRole.USER);
        return users.saveAndFlush(user);
    }

    private CompanyEntity saveCompany() {
        return companies.saveAndFlush(new CompanyEntity(
                "Company-" + UUID.randomUUID(),
                null,
                "Unspecified"
        ));
    }
}
