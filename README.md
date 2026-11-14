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
- Validate API request bodies and return timestamped, structured JSON errors.
- Use a Spring-managed job repository and service for listing, ID lookup, company
  search, and minimum-salary filtering.

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
- SLF4J with the SLF4J Simple logging implementation
- JUnit Jupiter

## Folder structure

```text
src/
  main/
    java/com/jobflow/
      JobFlowApplication
      api/            JobRequest, JobResponse, SalaryUpdateRequest,
                      StatusUpdateRequest
      app/            Main
      config/         AppConfig
      controller/    HelloController, JobController
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
    resources/
      application.properties
  test/
    java/com/jobflow/
      algorithm/      ArrayAlgorithmsTest, DataStructuresTest,
                      SearchingSortingHashingTest
      service/        JobServiceTest
pom.xml
README.md
```

## Setup

Install JDK 25 or newer and Maven, then run commands from the project root.

## Build and run

```powershell
mvn clean package
mvn exec:java
```

Run the unit tests with:

```powershell
mvn test
```

Start the REST API with:

```powershell
mvn spring-boot:run
```

The endpoints are available at `http://localhost:8080/hello` and
`http://localhost:8080/api/jobs`. The job API supports:

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

## Future improvements

- Persist job listings in a database.
- Add validation and configuration defaults.
- Expand unit and integration test coverage.
- Add a user interface and job-seeker-specific application history.
