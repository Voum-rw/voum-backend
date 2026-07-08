-- V12: Switch to phone+password authentication
-- Add hashed password column (nullable for safe migration of existing rows)
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Add FCM device token for push notifications
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);

-- Add preferred language (en | fr | rw)
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(5) NOT NULL DEFAULT 'en';
