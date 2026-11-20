USE jobflow;

INSERT INTO companies (id, name, website, country) VALUES
    (1, 'Northstar Labs', 'https://northstar.example', 'Germany'),
    (2, 'Cedar Analytics', 'https://cedar.example', 'United Kingdom'),
    (3, 'Orbit Cloudworks', 'https://orbit.example', 'Netherlands'),
    (4, 'Bluebird HealthTech', 'https://bluebird.example', 'Ireland'),
    (5, 'Lantern Mobility', 'https://lantern.example', 'Sweden'),
    (6, 'Maple Robotics', 'https://maple.example', 'Canada');

INSERT INTO users (id, name, email) VALUES
    (1, 'Alex Morgan', 'alex.morgan@example.test'),
    (2, 'Sam Rivera', 'sam.rivera@example.test'),
    (3, 'Taylor Kim', 'taylor.kim@example.test');

INSERT INTO jobs (id, company_id, title, location, salary) VALUES
    (1, 1, 'Java Developer', 'Berlin', 128000.00),
    (2, 1, 'Backend Java Developer', 'London', 142000.00),
    (3, 1, 'Product Designer', 'Hamburg', 92000.00),
    (4, 2, 'Java Developer', 'Berlin', 135000.00),
    (5, 2, 'Data Engineer', 'London', 150000.00),
    (6, 2, 'QA Engineer', 'Munich', 88000.00),
    (7, 3, 'Senior Java Developer', 'London', 175000.00),
    (8, 3, 'DevOps Engineer', 'Berlin', 160000.00),
    (9, 3, 'Product Analyst', 'Amsterdam', 110000.00),
    (10, 4, 'Java Developer', 'London', 118000.00),
    (11, 4, 'Security Analyst', 'Dublin', 130000.00),
    (12, 4, 'UX Designer', 'Cork', 90000.00),
    (13, 5, 'Java Software Engineer', 'Berlin', 155000.00),
    (14, 5, 'Data Scientist', 'London', 145000.00),
    (15, 5, 'Operations Analyst', 'Stockholm', 75000.00);

INSERT INTO applications (id, user_id, job_id, status) VALUES
    (1, 1, 1, 'APPLIED'),
    (2, 1, 4, 'INTERVIEW'),
    (3, 1, 7, 'APPLIED'),
    (4, 1, 10, 'REJECTED'),
    (5, 2, 2, 'APPLIED'),
    (6, 2, 5, 'INTERVIEW'),
    (7, 2, 8, 'APPLIED'),
    (8, 2, 13, 'APPLIED');
