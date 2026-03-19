-- ========================================================
-- V2__Alter_Skills_Level_To_Integer.sql
-- Changes skills.level from VARCHAR to INTEGER
-- to match the frontend numeric level contract:
--   1 = Beginner, 2 = Intermediate, 3 = Advanced, 4 = Expert
-- Existing VARCHAR data is safely migrated:
--   any non-numeric value is treated as 0 (unknown)
-- ========================================================

ALTER TABLE skills
ALTER COLUMN level TYPE INTEGER USING CASE
    WHEN level ~ '^\d+$' THEN level::INTEGER
    ELSE 0
END;

-- Add the composite index required for fast admin queries
CREATE INDEX IF NOT EXISTS idx_projects_user_deleted ON projects (user_id, deleted);

CREATE INDEX IF NOT EXISTS idx_experiences_user_deleted ON experiences (user_id, deleted);