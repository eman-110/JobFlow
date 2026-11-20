# JobFlow

JobFlow is a Java application for managing and exploring job listings. It demonstrates object-oriented design, generic repositories, dependency injection, streams, logging, Maven, and common data structures and algorithms.

## Features

- Add, list, search, and sort job listings by salary.
- Filter jobs by company, title, and minimum salary; calculate total salaries.
- Track application status and prevent duplicate applications.
- Simulate email and WhatsApp notifications.
- Load application settings from `application.properties`.
- Protect repository data with unmodifiable list copies.
- Serve REST endpoints for job listing CRUD, salary updates, application status,
  search, and filtering with Spring Boot.
- Report application name, version, and active environment from `/api/info`.
- Validate API request bodies and return timestamped, structured JSON errors.
- Use a Spring-managed job repository and service for listing, ID lookup, company
  search, and minimum-salary filtering.
- Configure application properties and development, test, and production
  profiles, including capacity and notification settings.
- Define standalone Jakarta Persistence entities and JPQL practice queries
  without connecting a database to Spring Boot.

## Data structures and algorithms

The `com.jobflow.algorithm` package includes:

- **Searching:** linear search and binary search on sorted arrays.
- **Sorting:** bubble sort, selection sort, insertion sort, and merge sort.
- **Hashing:** optimized two-sum, duplicate detection, frequency counting, first non-repeating character, and grouping anagrams.
- **Linked lists:** singly linked and doubly linked list operations, including reversal and finding the middle node.
- **Stacks and queues:** an array-backed stack and a circular array-backed queue.
- **Array algorithms:** searching, aggregation, reversal, and two-sum examples.

`JobService.sortJobsBySalaryAscending()` demonstrates sorting job results with Java's comparator without changing repository order.

## Coding interview practice

The current interview-style algorithm examples include:

- Validating parentheses and brackets with a stack.
- Finding two numbers that sum to a target with a hash map.
- Finding the first non-repeating character and grouping anagrams with hashing.
- Finding the middle linked-list node using slow and fast pointers.

Algorithm tests cover empty and already-sorted arrays, duplicates, negative values, and data-structure boundary cases.

## Technologies

- Java 25 (LTS)
- Apache Maven
- Spring Boot 4.1.2
- Jakarta Persistence
- Spring Web MVC
- Jakarta Bean Validation
- SLF4J with Logback
- JUnit Jupiter, Mockito, and Spring MVC Test
- JaCoCo

## Folder structure

```text
src/
  main/
    java/com/jobflow/
      JobFlowApplication
      api/            ApiErrorResponse, ApplicationInfo, JobRequest,
                      JobResponse, SalaryUpdateRequest, StatusUpdateRequest
      app/            Main
      config/         AppConfig, JobFlowProperties
      controller/     ApplicationInfoController, HelloController, JobController
      exception/      ApiExceptionHandler, JobNotFoundException
      model/          Job, JobSeeker, JobStatus, User
      notification/   EmailNotification, NotificationService,
                      SmsNotification, WhatsAppNotification
      repository/     JobRepository, Repository
      service/        JobService
      algorithm/      ArrayAlgorithms, ArrayQueue, ArrayStack,
                      DoublyLinkedList, HashingAlgorithms,
                      SearchingAlgorithms, SinglyLinkedList,
                      SortingAlgorithms, ValidParentheses
      persistence/entity/
                        CompanyEntity, JobEntity, UserEntity,
                        ApplicationEntity, JobEntityMapper
    resources/
      application.properties, application-dev.properties,
      application-test.properties, application-prod.properties
  test/
    java/com/jobflow/
      algorithm/      Existing algorithm tests
      config/         JobFlowConfigurationTest, TestProfileTest
      controller/     JobFlowApiTest, JobControllerTest
      model/          JobTest
      repository/     JobRepositoryTest
      service/        JobServiceTest, JobServiceMockitoTest
      persistence/entity/
                        CompanyEntityTest, JobEntityTest,
                        UserEntityTest, ApplicationEntityTest,
                        JobEntityMapperTest
database/
  schema.sql, seed.sql, queries.sql, jpql_queries.md
pom.xml
README.md
.github/workflows/ci.yml
```

## Setup

Install JDK 25 or newer, then run commands from the project root. Maven Wrapper
is included, so a global Maven installation is not required.

## Build and run

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd exec:java
```

## Testing and coverage

Run the complete test suite:

```powershell
.\mvnw.cmd clean test
```

Tests cover service behavior with Mockito, MVC behavior with MockMvc, structured
400/404/409 responses, request validation, profile configuration, and salary
boundaries. The integration test context is reset after every test method to
prevent in-memory repository data from leaking between tests.

Generate and review the JaCoCo coverage report:

```powershell
.\mvnw.cmd clean verify
```

Open `target/site/jacoco/index.html` to review line and branch coverage.

Run with a specific profile, for example:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test"
```

The test profile disables notifications and sets the maximum job capacity to
four. The development profile is the default; production and test settings are
defined in their respective `application-<profile>.properties` files.
The `JOBFLOW_PROFILE`, `JOBFLOW_APP_NAME`, `JOBFLOW_APP_VERSION`,
`JOBFLOW_ENVIRONMENT`, `JOBFLOW_NOTIFICATIONS_ENABLED`, `JOBFLOW_MAX_JOBS`, and
`JOBFLOW_MINIMUM_SALARY` environment variables override the default application
configuration.

## REST API

The endpoints are available at `http://localhost:8080/hello`,
`http://localhost:8080/api/info`, and `http://localhost:8080/api/jobs`. The job
API supports:

- `POST /api/jobs` with `title`, `company`, `location`, and `salary` to create
  a listing (returns `201 Created`).
- `GET /api/jobs` and `GET /api/jobs/{id}` to list or retrieve jobs.
- `PUT /api/jobs/{id}` to replace listing details.
- `PATCH /api/jobs/{id}/salary` with `{"salary": 125000}` to change salary only.
- `PATCH /api/jobs/{id}/status` with `{"status": "APPLIED"}` to update
  application status. Valid transitions are `SAVED -> APPLIED`,
  `APPLIED -> INTERVIEW|REJECTED`, and `INTERVIEW -> REJECTED`.
- `DELETE /api/jobs/{id}` to remove a listing.
- `GET /api/jobs/search?company=Tech%20Solutions` and
  `GET /api/jobs/high-paying?minimumSalary=150000` for filtering.

Request bodies are validated: text fields must be nonblank and at most 100
characters, and salaries must be present and non-negative. Errors use a JSON
object with `status`, `message`, `timestamp`, and `errors` fields; malformed
request bodies return HTTP 400 with a safe message.

Application logging uses Logback and can be configured through the profile
properties. The development profile writes `logs/jobflow.log`; generated logs
and Maven outputs are ignored by Git.

GitHub Actions runs `./mvnw clean verify` on pushes and pull requests targeting
`main`.

## Relational database practice

Standalone MySQL practice scripts are in [database/](./database/). They define
the four-table schema, insert fictional sample records, and provide the 20
assignment queries plus three bonus queries. The schema enforces non-negative
salaries and prevents duplicate applications by the same user for a job.

With MySQL 8.0.16 or newer installed, open a MySQL client from the repository
root and run the scripts in order:

```text
SOURCE database/schema.sql;
SOURCE database/seed.sql;
SOURCE database/queries.sql;
```

The schema creates and selects the `jobflow` database. `queries.sql` includes
example updates and a delete, so run it once after seeding. These scripts are
for database practice only and do not connect MySQL to Spring Boot.

The Jakarta Persistence entity layer is in
`src/main/java/com/jobflow/persistence/entity/`. Entities map to the standalone
schema and include pre-persist timestamps, explicit enum string mappings, and
the application composite unique constraint. `JobEntityMapper` takes a
company ID or company name resolved by its caller; it does not perform a
database lookup. JPQL exercises are in `database/jpql_queries.md`. No datasource
is configured, and no persistence tests connect to a database.

## Future improvements

- Connect the standalone MySQL practice schema to Spring Boot in a later lesson.
- Add a user interface and job-seeker-specific application history.
