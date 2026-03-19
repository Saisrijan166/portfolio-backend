CREATE INDEX IF NOT EXISTS idx_educations_user_deleted ON educations (user_id, deleted);
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_contacts_user_id ON contacts (user_id);
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes (user_id);
