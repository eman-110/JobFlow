# JobFlow JPQL practice queries

These queries use entity names and Java field names, not database table or
column names. Parameters are shown in JPQL named-parameter form and can be
bound by the caller.

1. Retrieve all jobs

   ```jpql
   SELECT j FROM JobEntity j
   ```

2. Find a job by ID (`:id`)

   ```jpql
   SELECT j FROM JobEntity j WHERE j.id = :id
   ```

3. Find jobs by company ID (`:companyId`)

   ```jpql
   SELECT j FROM JobEntity j WHERE j.companyId = :companyId
   ```

4. Find Java Developer jobs

   ```jpql
   SELECT j FROM JobEntity j WHERE j.title LIKE '%Java Developer%'
   ```

5. Find jobs paying above 100,000

   ```jpql
   SELECT j FROM JobEntity j WHERE j.salary > 100000
   ```

6. Sort jobs by salary descending

   ```jpql
   SELECT j FROM JobEntity j ORDER BY j.salary DESC
   ```

7. Count all jobs

   ```jpql
   SELECT COUNT(j) FROM JobEntity j
   ```

8. Retrieve all saved jobs

   ```jpql
   SELECT j FROM JobEntity j WHERE j.status = com.jobflow.model.JobStatus.SAVED
   ```

9. Find users by email (`:email`)

   ```jpql
   SELECT u FROM UserEntity u WHERE u.email = :email
   ```

10. Retrieve applications belonging to a particular user (`:userId`)

    ```jpql
    SELECT a FROM ApplicationEntity a WHERE a.userId = :userId
    ```
