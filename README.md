# JobFlow

JobFlow is a Java application for managing and exploring job listings. It demonstrates object-oriented design, generic repositories, dependency injection, streams, logging, Maven, and common data structures and algorithms.

## Features

- Add, list, search, and sort job listings by salary.
- Filter jobs by company, title, and minimum salary; calculate total salaries.
- Track application status and prevent duplicate applications.
- Simulate email and WhatsApp notifications.
- Load application settings from `application.properties`.
- Protect repository data with unmodifiable list copies.
- Serve sample REST endpoints for a greeting and job listings with Spring Boot.
- Use a Spring-managed job repository and service for listing, ID lookup, company
  search, and minimum-salary filtering.
- Create, update, and delete jobs through validated REST endpoints, with
  not-found and capacity errors returned as HTTP 404 and 409.

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
- Spring Web MVC
- Jakarta Bean Validation
- JUnit Jupiter, Mockito, and Spring MVC Test
- SLF4J with Spring Boot's default Logback implementation

## Folder structure

```text
src/
  main/
    java/com/jobflow/
      JobFlowApplication
      app/            Main
      config/         AppConfig
      controller/     HelloController, JobController
      exception/      JobNotFoundException, JobCapacityExceededException,
                      JobExceptionHandler
      api/            JobRequest, JobResponse
      model/          Job, JobSeeker, JobStatus, User
      notification/   EmailNotification, NotificationService,
                      SmsNotification, WhatsAppNotification
      repository/     JobRepository, Repository
      service/        JobService
      algorithm/      ArrayAlgorithms, ArrayQueue, ArrayStack,
                      DoublyLinkedList, HashingAlgorithms,
                      SearchingAlgorithms, SinglyLinkedList,
                      SortingAlgorithms, ValidParentheses
    resources/
      application.properties, application-test.properties
  test/
    java/com/jobflow/
      algorithm/      Existing algorithm tests
      config/         JobFlowConfigurationTest, TestProfileTest
      controller/     JobFlowApiTest, JobControllerTest
      model/          JobTest
      repository/     JobRepositoryTest
      service/        JobServiceTest, JobServiceMockitoTest
pom.xml
README.md
.github/workflows/ci.yml
```

## Setup

Install JDK 25 or newer and Maven, then run commands from the project root.

## Build and run

```powershell
mvn clean package
mvn exec:java
```

## Testing and coverage

Run the complete test suite:

```powershell
mvn clean test
```

The tests cover service behavior with Mockito, MVC behavior with MockMvc, request
validation, repository isolation, salary boundaries, and the `test` profile.
The test profile sets a four-job capacity and disables notifications.

Generate and review the JaCoCo coverage report:

```powershell
mvn clean verify
```

Open `target/site/jacoco/index.html` to review line and branch coverage.

Spring Boot uses Logback by default for application logging. Log levels can be
configured in `application.properties`; notification implementations currently
print their simulated messages to the console.

Run the API with the test profile using:

```powershell
mvn -Dspring-boot.run.profiles=test spring-boot:run
```

## Run the REST API

```powershell
mvn spring-boot:run
```

The sample endpoints are available at `http://localhost:8080/hello`,
`http://localhost:8080/api/jobs`, `http://localhost:8080/api/jobs/101`,
`http://localhost:8080/api/jobs/search?company=Tech%20Solutions`, and
`http://localhost:8080/api/jobs/high-paying?minimumSalary=150000`.

The GitHub Actions workflow runs `mvn clean verify` on pushes and pull requests
targeting `main`.

## Future improvements

- Persist job listings in a database.
- Expand unit and integration test coverage.
- Add a user interface and job-seeker-specific application history.
