USE jobflow;

-- 1. Retrieve all jobs.
SELECT *
FROM jobs;

-- 2. Find Java Developer jobs.
SELECT *
FROM jobs
WHERE title LIKE '%Java Developer%';

-- 3. Find jobs paying above 100,000.
SELECT *
FROM jobs
WHERE salary > 100000;

-- 4. Find jobs in Berlin or London.
SELECT *
FROM jobs
WHERE location IN ('Berlin', 'London');

-- 5. Find the five highest-paying jobs.
SELECT *
FROM jobs
ORDER BY salary DESC
LIMIT 5;

-- 6. Calculate the average salary.
SELECT AVG(salary) AS average_salary
FROM jobs;

-- 7. Find the highest and lowest salaries.
SELECT MAX(salary) AS highest_salary, MIN(salary) AS lowest_salary
FROM jobs;

-- 8. Count jobs for each company, including companies with no jobs.
SELECT c.id, c.name, COUNT(j.id) AS job_count
FROM companies AS c
LEFT JOIN jobs AS j ON j.company_id = c.id
GROUP BY c.id, c.name
ORDER BY c.id;

-- 9. Find companies with at least three jobs.
SELECT c.id, c.name, COUNT(j.id) AS job_count
FROM companies AS c
INNER JOIN jobs AS j ON j.company_id = c.id
GROUP BY c.id, c.name
HAVING COUNT(j.id) >= 3;

-- 10. Retrieve jobs with their company names.
SELECT j.id, j.title, j.location, j.salary, c.name AS company_name
FROM jobs AS j
INNER JOIN companies AS c ON c.id = j.company_id;

-- 11. Retrieve every company, including companies without jobs.
SELECT c.id, c.name, j.id AS job_id, j.title
FROM companies AS c
LEFT JOIN jobs AS j ON j.company_id = c.id
ORDER BY c.id, j.id;

-- 12. Display user names with the titles of jobs they applied for.
SELECT u.name AS user_name, j.title AS job_title, a.status AS application_status
FROM users AS u
INNER JOIN applications AS a ON a.user_id = u.id
INNER JOIN jobs AS j ON j.id = a.job_id
ORDER BY u.id, a.id;

-- 13. Find jobs paying more than the average salary.
SELECT id, title, salary
FROM jobs
WHERE salary > (SELECT AVG(salary) FROM jobs);

-- 14. Find users who have not applied for any job.
SELECT u.id, u.name, u.email
FROM users AS u
WHERE NOT EXISTS (
    SELECT 1
    FROM applications AS a
    WHERE a.user_id = u.id
);

-- 15. Update a job salary.
UPDATE jobs
SET salary = 130000.00
WHERE id = 1;

-- 16. Update an application status.
UPDATE applications
SET status = 'INTERVIEW'
WHERE id = 1;

-- 17. Delete a selected application.
DELETE FROM applications
WHERE id = 8;

-- 18. Create and query a view for jobs paying at least 150,000.
CREATE OR REPLACE VIEW high_paying_jobs AS
SELECT id, company_id, title, location, salary, status, created_at
FROM jobs
WHERE salary >= 150000;

SELECT *
FROM high_paying_jobs
ORDER BY salary DESC;

-- 19. Create a salary index and inspect the query plan.
CREATE INDEX idx_jobs_salary ON jobs (salary);

EXPLAIN
SELECT id, title, salary
FROM jobs
WHERE salary >= 150000;

-- 20. Demonstrate a transaction rollback; job 2's salary remains unchanged.
START TRANSACTION;

UPDATE jobs
SET salary = salary + 10000.00
WHERE id = 2;

SELECT id, title, salary
FROM jobs
WHERE id = 2;

ROLLBACK;

SELECT id, title, salary
FROM jobs
WHERE id = 2;

-- Bonus 1. Find the second-highest distinct salary without LIMIT.
SELECT MAX(salary) AS second_highest_salary
FROM jobs
WHERE salary < (
    SELECT MAX(salary)
    FROM jobs
);

-- Bonus 2. Rank all job rows by salary without eliminating ties.
SELECT
    title,
    salary,
    DENSE_RANK() OVER (ORDER BY salary DESC) AS salary_rank
FROM jobs;

-- Bonus 3. Categorize jobs according to salary.
SELECT
    title,
    salary,
    CASE
        WHEN salary >= 150000 THEN 'High'
        WHEN salary >= 100000 THEN 'Medium'
        ELSE 'Low'
    END AS salary_category
FROM jobs;
