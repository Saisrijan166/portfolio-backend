-- ========================================================
-- V1__Initial_Schema.sql
-- Consolidated Initial Schema for Portfolio Platform
-- ========================================================

-- USERS
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_username ON users (username);

-- PROFILES
CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255),
    role_title VARCHAR(255),
    bio VARCHAR(2000),
    image VARCHAR(255),
    location VARCHAR(255),
    availability VARCHAR(255),
    experience_years VARCHAR(255),
    principles TEXT,
    about TEXT,
    os_name VARCHAR(255),
    account_type VARCHAR(255),
    access VARCHAR(255),
    role_description VARCHAR(255),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- PROJECTS
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255),
    type VARCHAR(255),
    status VARCHAR(255),
    year VARCHAR(255),
    overview VARCHAR(2000),
    live_link VARCHAR(255),
    source_link VARCHAR(255),
    pdf_link VARCHAR(255),
    media_video VARCHAR(255),
    is_research BOOLEAN DEFAULT FALSE NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_projects_user_id ON projects (user_id);

-- PROJECT SCREENSHOTS
CREATE TABLE project_screenshots (
    project_id BIGINT NOT NULL,
    screenshot_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_project_screenshots_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

-- PROJECT TECH STACK
CREATE TABLE project_tech_stacks (
    project_id BIGINT NOT NULL,
    tech VARCHAR(255) NOT NULL,
    CONSTRAINT fk_project_tech_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
);

-- EXPERIENCES
CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company VARCHAR(255),
    role_title VARCHAR(255),
    duration VARCHAR(255),
    start_year INTEGER,
    end_year INTEGER,
    is_current BOOLEAN DEFAULT FALSE NOT NULL,
    is_academic BOOLEAN DEFAULT FALSE NOT NULL,
    level VARCHAR(255),
    institute VARCHAR(255),
    location VARCHAR(255),
    degree VARCHAR(255),
    score_label VARCHAR(255),
    score_value VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_experiences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_experiences_user_id ON experiences (user_id);

-- EXPERIENCE ACHIEVEMENTS
CREATE TABLE experience_achievements (
    experience_id BIGINT NOT NULL,
    achievement VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_exp_achievements FOREIGN KEY (experience_id) REFERENCES experiences (id) ON DELETE CASCADE
);

-- EXPERIENCE RESPONSIBILITIES
CREATE TABLE experience_responsibilities (
    experience_id BIGINT NOT NULL,
    responsibility VARCHAR(1000) NOT NULL,
    CONSTRAINT fk_exp_responsibilities FOREIGN KEY (experience_id) REFERENCES experiences (id) ON DELETE CASCADE
);

-- EXPERIENCE SKILLS
CREATE TABLE experience_skills (
    experience_id BIGINT NOT NULL,
    skill VARCHAR(255) NOT NULL,
    CONSTRAINT fk_exp_skills FOREIGN KEY (experience_id) REFERENCES experiences (id) ON DELETE CASCADE
);

-- SKILLS
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    domain VARCHAR(255),
    name VARCHAR(255),
    level VARCHAR(255),
    is_meta_skill BOOLEAN DEFAULT FALSE NOT NULL,
    meta_description VARCHAR(1000),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_skills_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_skills_user_id ON skills (user_id);

-- EDUCATIONS
CREATE TABLE educations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    level VARCHAR(255),
    institute VARCHAR(255),
    location VARCHAR(255),
    degree VARCHAR(255),
    score_label VARCHAR(255),
    score_value VARCHAR(255),
    duration VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_education_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- RESUMES
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    resume_url VARCHAR(255),
    last_updated VARCHAR(255),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- CONTACTS
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    primary_email VARCHAR(255),
    created_at TIMESTAMP DEFAULT now() NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_contact_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- CONTACT SOCIAL LINKS
CREATE TABLE contact_social_links (
    contact_id BIGINT NOT NULL,
    label VARCHAR(255),
    url VARCHAR(255),
    CONSTRAINT fk_contact_social_links FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE
);

-- CONTACT PROFESSIONAL LINKS
CREATE TABLE contact_professional_links (
    contact_id BIGINT NOT NULL,
    label VARCHAR(255),
    url VARCHAR(255),
    CONSTRAINT fk_contact_prof_links FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE
);

-- REFRESH TOKENS
CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_token ON refresh_token (token);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);