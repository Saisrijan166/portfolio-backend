BEGIN;

CREATE TABLE IF NOT EXISTS abouts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100),
    role_title VARCHAR(100),
    bio VARCHAR(500),
    image VARCHAR(512),
    location VARCHAR(50),
    availability VARCHAR(50),
    experience_years VARCHAR(50),
    about_content TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS about_principles (
    id BIGSERIAL PRIMARY KEY,
    about_id BIGINT NOT NULL REFERENCES abouts(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    title VARCHAR(60) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_about_principles_about_sort
    ON about_principles (about_id, sort_order);

UPDATE profiles
SET
    profile_name = NULLIF(profile_name, ''),
    profile_role_title = NULLIF(profile_role_title, ''),
    profile_location = NULLIF(profile_location, ''),
    profile_availability = NULLIF(profile_availability, '')
WHERE
    profile_name = ''
    OR profile_role_title = ''
    OR profile_location = ''
    OR profile_availability = '';

INSERT INTO abouts (
    user_id,
    name,
    role_title,
    bio,
    image,
    location,
    availability,
    experience_years,
    about_content,
    created_at,
    updated_at
)
SELECT
    p.user_id,
    LEFT(p.profile_name, 100),
    LEFT(p.profile_role_title, 100),
    NULL,
    NULL,
    LEFT(p.profile_location, 50),
    LEFT(p.profile_availability, 50),
    NULL,
    NULL,
    p.created_at,
    COALESCE(p.updated_at, NOW())
FROM profiles p
ON CONFLICT (user_id) DO UPDATE
SET
    name = COALESCE(abouts.name, EXCLUDED.name),
    role_title = COALESCE(abouts.role_title, EXCLUDED.role_title),
    location = COALESCE(abouts.location, EXCLUDED.location),
    availability = COALESCE(abouts.availability, EXCLUDED.availability),
    updated_at = NOW();

ALTER TABLE profiles
    DROP COLUMN IF EXISTS name,
    DROP COLUMN IF EXISTS role_title,
    DROP COLUMN IF EXISTS bio,
    DROP COLUMN IF EXISTS image,
    DROP COLUMN IF EXISTS location,
    DROP COLUMN IF EXISTS availability,
    DROP COLUMN IF EXISTS experience_years,
    DROP COLUMN IF EXISTS about,
    DROP COLUMN IF EXISTS principles,
    DROP COLUMN IF EXISTS about_name,
    DROP COLUMN IF EXISTS about_role_title,
    DROP COLUMN IF EXISTS about_bio,
    DROP COLUMN IF EXISTS about_image,
    DROP COLUMN IF EXISTS about_location,
    DROP COLUMN IF EXISTS about_availability,
    DROP COLUMN IF EXISTS about_experience_years;

COMMIT;






BEGIN;

ALTER TABLE profiles
    ALTER COLUMN profile_name TYPE VARCHAR(100) USING LEFT(profile_name, 100),
    ALTER COLUMN profile_role_title TYPE VARCHAR(100) USING LEFT(profile_role_title, 100),
    ALTER COLUMN profile_location TYPE VARCHAR(50) USING LEFT(profile_location, 50),
    ALTER COLUMN profile_availability TYPE VARCHAR(50) USING LEFT(profile_availability, 50),
    ALTER COLUMN profile_primary_email TYPE VARCHAR(255) USING LEFT(profile_primary_email, 255),
    ALTER COLUMN os_name TYPE VARCHAR(100) USING LEFT(os_name, 100),
    ALTER COLUMN account_type TYPE VARCHAR(100) USING LEFT(account_type, 100),
    ALTER COLUMN access TYPE VARCHAR(100) USING LEFT(access, 100),
    ALTER COLUMN role_description TYPE VARCHAR(100) USING LEFT(role_description, 100);

ALTER TABLE abouts
    ALTER COLUMN name TYPE VARCHAR(100) USING LEFT(name, 100),
    ALTER COLUMN role_title TYPE VARCHAR(100) USING LEFT(role_title, 100),
    ALTER COLUMN bio TYPE VARCHAR(500) USING LEFT(bio, 500),
    ALTER COLUMN image TYPE VARCHAR(512) USING LEFT(image, 512),
    ALTER COLUMN location TYPE VARCHAR(50) USING LEFT(location, 50),
    ALTER COLUMN availability TYPE VARCHAR(50) USING LEFT(availability, 50),
    ALTER COLUMN experience_years TYPE VARCHAR(50) USING LEFT(experience_years, 50);

ALTER TABLE about_principles
    ALTER COLUMN title TYPE VARCHAR(60) USING LEFT(title, 60),
    ALTER COLUMN description TYPE VARCHAR(255) USING LEFT(description, 255);

ALTER TABLE projects
    ALTER COLUMN name TYPE VARCHAR(100) USING LEFT(name, 100),
    ALTER COLUMN type TYPE VARCHAR(100) USING LEFT(type, 100),
    ALTER COLUMN status TYPE VARCHAR(100) USING LEFT(status, 100),
    ALTER COLUMN year TYPE VARCHAR(100) USING LEFT(year, 100),
    ALTER COLUMN overview TYPE VARCHAR(3000) USING LEFT(overview, 3000),
    ALTER COLUMN live_link TYPE VARCHAR(512) USING LEFT(live_link, 512),
    ALTER COLUMN source_link TYPE VARCHAR(512) USING LEFT(source_link, 512),
    ALTER COLUMN pdf_link TYPE VARCHAR(512) USING LEFT(pdf_link, 512),
    ALTER COLUMN media_video TYPE VARCHAR(512) USING LEFT(media_video, 512);

ALTER TABLE project_tech_stacks
    ALTER COLUMN tech TYPE VARCHAR(255) USING LEFT(tech, 255);

ALTER TABLE project_screenshots
    ALTER COLUMN screenshot_url TYPE VARCHAR(512) USING LEFT(screenshot_url, 512);

ALTER TABLE experiences
    ADD COLUMN IF NOT EXISTS start_month INTEGER,
    ADD COLUMN IF NOT EXISTS end_month INTEGER;

ALTER TABLE experiences
    ALTER COLUMN company TYPE VARCHAR(100) USING LEFT(company, 100),
    ALTER COLUMN role_title TYPE VARCHAR(100) USING LEFT(role_title, 100),
    ALTER COLUMN duration TYPE VARCHAR(100) USING LEFT(duration, 100),
    ALTER COLUMN level TYPE VARCHAR(100) USING LEFT(level, 100),
    ALTER COLUMN institute TYPE VARCHAR(100) USING LEFT(institute, 100),
    ALTER COLUMN location TYPE VARCHAR(100) USING LEFT(location, 100),
    ALTER COLUMN degree TYPE VARCHAR(100) USING LEFT(degree, 100),
    ALTER COLUMN score_label TYPE VARCHAR(100) USING LEFT(score_label, 100),
    ALTER COLUMN score_value TYPE VARCHAR(100) USING LEFT(score_value, 100);

ALTER TABLE experiences
    DROP CONSTRAINT IF EXISTS chk_experiences_start_month,
    DROP CONSTRAINT IF EXISTS chk_experiences_end_month;

ALTER TABLE experiences
    ADD CONSTRAINT chk_experiences_start_month CHECK (start_month IS NULL OR start_month BETWEEN 1 AND 12),
    ADD CONSTRAINT chk_experiences_end_month CHECK (end_month IS NULL OR end_month BETWEEN 1 AND 12);

ALTER TABLE experience_responsibilities
    ALTER COLUMN responsibility TYPE VARCHAR(255) USING LEFT(responsibility, 255);

ALTER TABLE experience_achievements
    ALTER COLUMN achievement TYPE VARCHAR(255) USING LEFT(achievement, 255);

ALTER TABLE experience_skills
    ALTER COLUMN skill TYPE VARCHAR(255) USING LEFT(skill, 255);

ALTER TABLE skills
    ALTER COLUMN domain TYPE VARCHAR(100) USING LEFT(domain, 100),
    ALTER COLUMN name TYPE VARCHAR(100) USING LEFT(name, 100),
    ALTER COLUMN meta_description TYPE VARCHAR(500) USING LEFT(meta_description, 500);

ALTER TABLE educations
    ALTER COLUMN level TYPE VARCHAR(100) USING LEFT(level, 100),
    ALTER COLUMN institute TYPE VARCHAR(100) USING LEFT(institute, 100),
    ALTER COLUMN location TYPE VARCHAR(100) USING LEFT(location, 100),
    ALTER COLUMN degree TYPE VARCHAR(100) USING LEFT(degree, 100),
    ALTER COLUMN score_label TYPE VARCHAR(100) USING LEFT(score_label, 100),
    ALTER COLUMN score_value TYPE VARCHAR(100) USING LEFT(score_value, 100),
    ALTER COLUMN duration TYPE VARCHAR(100) USING LEFT(duration, 100);

ALTER TABLE contacts
    ALTER COLUMN primary_email TYPE VARCHAR(255) USING LEFT(primary_email, 255);

ALTER TABLE contact_professional_links
    ALTER COLUMN label TYPE VARCHAR(100) USING LEFT(label, 100),
    ALTER COLUMN url TYPE VARCHAR(512) USING LEFT(url, 512);

ALTER TABLE contact_social_links
    ALTER COLUMN label TYPE VARCHAR(100) USING LEFT(label, 100),
    ALTER COLUMN url TYPE VARCHAR(512) USING LEFT(url, 512);

ALTER TABLE resumes
    ALTER COLUMN resume_url TYPE VARCHAR(512) USING LEFT(resume_url, 512),
    ALTER COLUMN last_updated TYPE VARCHAR(255) USING LEFT(last_updated, 255);

COMMIT;
