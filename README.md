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
  and persist jobs to MySQL with Spring Data JPA and Flyway migrations.
- Protect job mutations and application submissions with role-based Spring
  Security, BCrypt password hashing, and MySQL-backed user credentials.
- Use signed HS256 JWT access tokens and rotating opaque refresh credentials.
- Cache public job listings and job details in Redis when using the MySQL profile.

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
- Spring Security
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
      api/            ApiErrorResponse, ApplicationInfo, ApplicationResponse,
                      CsrfTokenResponse, JobRequest, JobResponse,
                      SalaryUpdateRequest, StatusUpdateRequest
      app/            Main
      config/         AppConfig, JobFlowProperties, SecurityConfig
      controller/     ApplicationInfoController, CsrfTokenController,
                      HelloController, JobApplicationController, JobController
      exception/      ApiExceptionHandler, JobNotFoundException
      model/          Job, JobSeeker, JobStatus, User
      notification/   EmailNotification, NotificationService,
                      SmsNotification, WhatsAppNotification
      repository/     JobRepository, JobStore, Repository
      service/        JobService
      algorithm/      ArrayAlgorithms, ArrayQueue, ArrayStack,
                      DoublyLinkedList, HashingAlgorithms,
                      SearchingAlgorithms, SinglyLinkedList,
                      SortingAlgorithms, ValidParentheses
      persistence/entity/
                        CompanyEntity, JobEntity, UserEntity, UserRole,
                        ApplicationEntity, JobEntityMapper
      persistence/repository/
                        CompanyJpaRepository, JobJpaRepository,
                        UserJpaRepository, ApplicationJpaRepository,
                        JpaJobStore
    resources/
      application.properties, application-dev.properties,
      application-mysql.properties, application-test.properties,
      application-prod.properties, db/migration/
  test/
    java/com/jobflow/
      algorithm/      Existing algorithm tests
      config/         JobFlowConfigurationTest, TestProfileTest
      controller/     JobFlowApiTest, JobControllerTest
      model/          JobTest
      repository/     JobRepositoryTest
      service/        DatabaseUserDetailsService, JobApplicationService,
                      JobServiceTest, JobServiceMockitoTest
      persistence/entity/
                        CompanyEntityTest, JobEntityTest,
                        UserEntityTest, ApplicationEntityTest,
                        JobEntityMapperTest
      persistence/repository/
                        JobJpaRepositoryIntegrationTest
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
boundaries. The API tests use the in-memory repository and do not require MySQL.
Security tests verify public reads, role-restricted job mutations, password
verification, and application ownership. The MySQL repository integration
tests use Testcontainers and require Docker; CI verifies Docker availability
and fails if any of the 42 integration tests are skipped.

Generate and review the JaCoCo coverage report:

```powershell
.\mvnw.cmd clean verify
```

Open `target/site/jacoco/index.html` to review line and branch coverage.

Run with a specific non-database profile, for example:

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

## MySQL persistence

The `dev`, `test`, and `prod` profiles continue to use the in-memory job store.
The `mysql` profile selects the Spring Data JPA implementation, uses Flyway to
create the schema, and keeps Hibernate in `validate` mode so schema mismatches
fail startup rather than being silently altered.

Create the databases and a restricted application account in MySQL (choose a
local password and do not commit it):

```sql
SOURCE database/mysql-setup.sql;
CREATE USER 'jobflow_app'@'localhost' IDENTIFIED BY 'your_local_password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON jobflow_dev.* TO 'jobflow_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON jobflow_test.* TO 'jobflow_app'@'localhost';
```

Flyway needs schema-creation permissions on the configured database. Keep the
account local and use a unique password. Set credentials and select the profile
in PowerShell:

```powershell
$env:DB_USERNAME = "jobflow_app"
$env:DB_PASSWORD = "your_local_password"
$env:JOBFLOW_PROFILE = "mysql"
.\mvnw.cmd spring-boot:run
```

By default the application connects to
`jdbc:mysql://localhost:3306/jobflow_dev`. Set `DB_URL` to override the JDBC
URL. MySQL Connector/J is included at runtime.
Company names are resolved using the database's case-insensitive unique index.
The MySQL profile rejects names that are not already present in `companies`;
it never silently creates a company or assigns a placeholder country. Create
company records through the persistence repository or insert them explicitly
before posting a job.

The paginated endpoint is `GET /api/jobs/page?page=0&size=20&sortBy=salary&direction=desc`.
Page numbers are zero-based, sizes are limited to 1–100, and sorting supports
`id`, `title`, `location`, or `salary`.

Run all tests with `.\mvnw.cmd clean verify`. Testcontainers launches a
disposable MySQL 8.4 instance for the repository suite. The MySQL integration
suite also requires Redis; CI starts a Redis 7 service for it. To run only those
tests, use `.\mvnw.cmd -Dtest=JobJpaRepositoryIntegrationTest test`; Docker and
a local Redis server are required. The suite verifies generated IDs, CRUD,
derived queries, JPQL, database constraints, pagination and sorting, external
identity persistence, one-time OAuth exchange replay/concurrency protections,
and Redis cache hits, eviction, expiration, and database consistency.

## Redis caching

Redis caching is enabled only with the `mysql` profile. The `dev` and `test`
profiles do not connect to Redis and continue to work without a Redis server.
Start a local Redis 7 container from PowerShell:

```powershell
docker run --name jobflow-redis -d -p 6379:6379 redis:7-alpine
docker exec jobflow-redis redis-cli ping
```

The second command should return `PONG`. Configure Redis with these optional
environment variables (defaults are suitable only for local development):

| Variable | Default | Description |
| --- | --- | --- |
| `REDIS_HOST` | `localhost` | Redis server host |
| `REDIS_PORT` | `6379` | Redis server port |
| `REDIS_PASSWORD` | empty | Password for a password-protected Redis server |

Production Redis should be private-network-only and protected with credentials
provided through the deployment environment or secret manager; do not commit
passwords or expose Redis publicly.

The `jobs` cache stores the public `GET /api/jobs` listing, and `jobDetails`
stores individual public `GET /api/jobs/{id}` results keyed by job ID. Both use
a five-minute TTL. Successful job creation, update (including salary/status
updates), and deletion evict the listing cache and the affected detail entry.
No authentication credentials or private user data are cached.

Run the MySQL integration tests with Docker and Redis available:

```powershell
.\mvnw.cmd -Dtest=JobJpaRepositoryIntegrationTest test
```

After startup, check the API and persistence manually:

```powershell
curl.exe http://localhost:8080/api/jobs
# Create a job with POST /api/jobs, stop and restart the app, then GET /api/jobs/{id}.
```

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
- `POST /api/applications/{jobId}` to submit an application as the authenticated
  user (MySQL profile only; the request never accepts a `userId`).

Request bodies are validated: text fields must be nonblank and at most 100
characters, and salaries must be present and non-negative. Errors use a JSON
object with `status`, `message`, `timestamp`, and `errors` fields; malformed
request bodies return HTTP 400 with a safe message.

Application logging uses Logback and can be configured through the profile
properties. The development profile writes `logs/jobflow.log`; generated logs
and Maven outputs are ignored by Git.

GitHub Actions runs `./mvnw clean verify` on pushes and pull requests targeting
`main`.

## Security

The REST API uses stateless bearer JWT authentication and the following
authorization rules:

| Operation | Access |
| --- | --- |
| `GET /api/jobs/**`, `/api/info`, `/hello` | Public |
| `POST /api/auth/register`, `/login`, `/refresh`, `/logout`, `/oauth/exchange` | Public |
| `POST /api/auth/oauth/link/start` | Authenticated |
| `GET /api/auth/me` | Authenticated |
| `POST`, `PUT`, or `PATCH /api/jobs/**` | `RECRUITER` or `ADMIN` |
| `DELETE /api/jobs/**` | `ADMIN` |
| `POST /api/applications/{jobId}` | `USER` |

Other routes and HTTP methods are denied by default. Job listing updates,
including the salary and status PATCH endpoints, require `RECRUITER` or
`ADMIN`. A new application is always associated with the authenticated
stable database user ID; a caller-supplied user ID is not used. The lower-level
ID-based application service operation is restricted to `ADMIN`, while
authenticated user submissions are guarded by method-level security.

The `dev` profile has three development-only in-memory accounts:
`jobseeker@jobflow.local` / `dev-user-password`,
`recruiter@jobflow.local` / `dev-recruiter-password`, and
`admin@jobflow.local` / `dev-admin-password`. Override usernames and passwords
with `JOBFLOW_DEV_USER_USERNAME`, `JOBFLOW_DEV_USER_PASSWORD`,
`JOBFLOW_DEV_RECRUITER_USERNAME`, `JOBFLOW_DEV_RECRUITER_PASSWORD`,
`JOBFLOW_DEV_ADMIN_USERNAME`, and `JOBFLOW_DEV_ADMIN_PASSWORD`. These defaults
are for local development only and are not enabled in the production profile.
Tests have separate test-only identities. Do not reuse the development
passwords outside local development.

Under the `mysql` profile, `DatabaseUserDetailsService` authenticates against
`users.email`, `users.password_hash`, and `users.role`. Flyway migration V2
adds those columns without modifying V1. Password hashes are BCrypt values;
existing users with no password hash cannot authenticate until provisioned
through a trusted administrative process. Never put plaintext passwords in
the database or commit production credentials. The current `prod` profile
fails closed because it has no production identity provider configured; wire
an approved production identity provider before enabling authenticated
operations there.

CSRF is disabled only on the stateless `/api/**` bearer-token chain, which
never reads authentication from cookies. The separate browser OAuth chain
retains Spring Security's CSRF protection and short-lived session for the
authorization callback and OAuth `state`; OIDC `nonce` and token validation
are handled by Spring Security. `/api/csrf` remains a compatibility endpoint
and does not enable cookie authentication.

Set `JWT_SECRET` to a Base64-encoded random secret of at least 32 decoded
bytes. Access tokens expire after 15 minutes by default
(`JWT_ACCESS_TOKEN_MINUTES`) and refresh credentials expire after seven days
(`JWT_REFRESH_TOKEN_DAYS`). JWT issuer/audience can be configured with
`JWT_ISSUER` and `JWT_AUDIENCE`. Every non-test profile requires `JWT_SECRET`;
only test configuration supplies an explicit test-only secret. Refresh
credentials are generated with 256 bits of randomness, SHA-256 hashed before
database storage, and rotated transactionally. Reuse of a rotated credential
revokes every token in its family before returning an unauthorized response.

### Optional Google OpenID Connect

Google sign-in is opt-in. Keep the existing profile configuration for normal
applications; enable it only alongside MySQL, for example:

```powershell
$env:JOBFLOW_PROFILE = "mysql,oauth"
$env:GOOGLE_CLIENT_ID = "your-google-oauth-client-id"
$env:GOOGLE_CLIENT_SECRET = "your-google-oauth-client-secret"
$env:OAUTH_APP_CLIENT_ID = "jobflow-web"
$env:OAUTH_AUTHORIZATION_BASE_URL = "http://localhost:8080"
$env:OAUTH_SUCCESS_REDIRECT_URI = "http://localhost:3000/auth/callback"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"
.\mvnw.cmd spring-boot:run
```

In Google Cloud, register the backend callback URI
`http://localhost:8080/login/oauth2/code/google` (use HTTPS outside local
development). Configure `OAUTH_SUCCESS_REDIRECT_URI` to a trusted frontend
callback. Redirects are fixed by server configuration, not by an OAuth request
parameter. Never commit Google secrets. With the `oauth` profile inactive,
Google credentials and OAuth client beans are not required.

The browser authorization chain is separate from the stateless REST resource
server. It uses Spring Security's authorization-code state handling, OIDC
nonce/signature/issuer/audience validation, and PKCE for the provider token
exchange. The frontend creates an independent PKCE verifier/challenge and
starts at `/oauth2/authorization/google?client_id=jobflow-web&code_challenge=<S256-challenge>`.
After successful provider validation, JobFlow redirects to the configured
frontend with only a short-lived, single-use `code` (never a JobFlow access or
refresh token). Exchange it with:

```http
POST /api/auth/oauth/exchange
Content-Type: application/json

{"code":"<one-time-code>","clientId":"jobflow-web","codeVerifier":"<pkce-verifier>"}
```

The code expires after 90 seconds. V4 stores only its SHA-256 hash, the client
identifier, and the PKCE challenge; a conditional database update atomically
consumes it before the existing access-token and refresh-token services issue
the normal JobFlow pair. A wrong client, verifier, expired code, or replay is
rejected.

New external-only accounts require a provider-verified email, receive only the
`USER` role, and have a null local password hash, so password login cannot
authenticate them. A matching email never causes automatic account linking.
To explicitly link an identity, an already authenticated local user posts a
PKCE challenge to `POST /api/auth/oauth/link/start` with its bearer token,
then completes a new Google flow. The one-use link intent is bound to the
authenticated database account and verifier; the provider is asked for fresh
authentication, and missing/stale `auth_time` fails closed. Existing account
ownership, issuer, subject, verified email and uniqueness are checked before
the identity is attached.

Flyway V4 adds `external_identities`, `oauth_login_exchanges`, and
`oauth_link_intents`; it does not rewrite V1–V3. Login/register/refresh rate
limits default to 10/5/30 requests per minute per source address and return
HTTP 429 with `Retry-After`. They are in-memory and per application instance;
use an external/distributed limiter for multi-instance deployments. CORS
origins are explicitly configurable with `CORS_ALLOWED_ORIGINS`; production
defaults to no cross-origin origins. REST responses include restrictive
security headers. Authentication audit events record event types only and
never credential values.

OAuth tests use controlled OIDC claim objects and never call Google or require
production credentials. The MySQL suite remains Docker/Testcontainers based.
GitHub OAuth, Redis-backed distributed rate limiting, MFA/TOTP, and a
monitoring dashboard are not included in this lesson implementation.

Example:

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:DB_USERNAME = "jobflow_app"
$env:DB_PASSWORD = "your_local_password"
$env:JOBFLOW_PROFILE = "mysql"
.\mvnw.cmd spring-boot:run
```

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
`src/main/java/com/jobflow/persistence/entity/`. Entities map to the
version-controlled Flyway schema and include pre-persist timestamps, explicit
enum string mappings, and the application composite unique constraint.
`JobEntity` references its `CompanyEntity`, while `ApplicationEntity` references
its `UserEntity` and `JobEntity`; the parent entities expose the corresponding
collections through `mappedBy` relationships. The MySQL-profile
`JobApplicationService` validates both records and duplicate applications and
saves within a transaction. The database unique constraint remains the final
guard against concurrent duplicate submissions.

The JPA repositories include company joins, fetch-join queries for relationship
loading, and a `JobSummary` DTO projection that avoids returning entity graphs.
The MySQL Testcontainers integration suite verifies relationship navigation,
duplicate protection, transaction rollback, and the SQL statement reduction
from a company fetch join. It also checks database-backed authentication and
that application submissions use the authenticated user's identity. Run
`.\mvnw.cmd clean verify`; CI requires all 40 MySQL integration tests to pass
without skips.

`JobEntityMapper` maps domain jobs and persisted records; `JpaJobStore` resolves
company entities and stores the MySQL-generated job ID in the API model. The
standalone SQL examples and JPQL exercises remain in `database/` and
`database/jpql_queries.md`.

## Future improvements

- Add a user interface and job-seeker-specific application history.
