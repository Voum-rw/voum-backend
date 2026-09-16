-- Fail on existing case-insensitive duplicates rather than silently merging accounts.
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (LOWER(email)) WHERE email IS NOT NULL;
