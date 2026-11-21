CREATE TABLE companies (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    website VARCHAR(255),
    country VARCHAR(100) NOT NULL,
    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE jobs (
    id INT NOT NULL AUTO_INCREMENT,
    company_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    location VARCHAR(150) NOT NULL,
    salary DECIMAL(12, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SAVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_jobs PRIMARY KEY (id),
    CONSTRAINT chk_jobs_salary_nonnegative CHECK (salary >= 0),
    CONSTRAINT chk_jobs_status
        CHECK (status IN ('SAVED', 'APPLIED', 'INTERVIEW', 'REJECTED')),
    CONSTRAINT fk_jobs_company
        FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE = InnoDB;

CREATE TABLE applications (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    job_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uq_applications_user_job UNIQUE (user_id, job_id),
    CONSTRAINT chk_applications_status
        CHECK (status IN ('SAVED', 'APPLIED', 'INTERVIEW', 'REJECTED')),
    CONSTRAINT fk_applications_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_applications_job
        FOREIGN KEY (job_id) REFERENCES jobs (id)
) ENGINE = InnoDB;
