# JobFlow

JobFlow is a small Java application for managing and exploring job listings. It demonstrates object-oriented design, generic repositories, dependency injection, streams, logging, and Maven.

## Features

- Add, list, search, and sort job listings.
- Filter jobs by company, title, and minimum salary; calculate total salaries.
- Track application status and prevent duplicate applications.
- Simulate email and WhatsApp notifications.
- Load application settings from `application.properties`.
- Protect repository data with unmodifiable list copies.

## Technologies

- Java 25 (LTS)
- Apache Maven
- SLF4J with the SLF4J Simple logging implementation
- JUnit Jupiter

## Folder structure

```text
src/
  main/
    java/com/jobflow/
      app/            Main
      config/         AppConfig
      exception/      JobNotFoundException
      model/          Job, JobSeeker, JobStatus, User
      notification/   EmailNotification, NotificationService,
                      SmsNotification, WhatsAppNotification
      repository/     Repository
      service/        JobService
    resources/
      application.properties
  test/
    java/com/jobflow/service/
      JobServiceTest.java
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

## Future improvements

- Persist job listings in a database.
- Add validation and configuration defaults.
- Expand unit and integration test coverage.
- Add a user interface and job-seeker-specific application history.
