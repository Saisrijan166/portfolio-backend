-- V2__add_new_portfolio_entities.sql

-- Add new columns to existing tables
ALTER TABLE profiles ADD COLUMN principles TEXT;

ALTER TABLE projects
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE experiences
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- Create Educations table
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
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_education_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Create Resumes table
CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    resume_url VARCHAR(255),
    last_updated VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Create Contacts table
CREATE TABLE contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    primary_email VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_contact_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Create Contact Social Links (ElementCollection)
CREATE TABLE contact_social_links (
    contact_id BIGINT NOT NULL,
    label VARCHAR(255),
    url VARCHAR(255),
    CONSTRAINT fk_contact_social_links FOREIGN KEY (contact_id) REFERENCES contacts (id)
);

-- Create Contact Professional Links (ElementCollection)
CREATE TABLE contact_professional_links (
    contact_id BIGINT NOT NULL,
    label VARCHAR(255),
    url VARCHAR(255),
    CONSTRAINT fk_contact_prof_links FOREIGN KEY (contact_id) REFERENCES contacts (id)
);