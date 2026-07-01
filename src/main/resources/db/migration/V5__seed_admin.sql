-- ============================================================
-- V5: Seed Default Admin User
-- ============================================================
-- This inserts a pre-known admin account that can authenticate
-- via the OTP bypass (code 123456 for @voum.com emails).
-- The UUID is fixed so this migration is idempotent.
-- ============================================================

INSERT INTO users (
    id,
    name,
    phone,
    email,
    role,
    is_verified,
    is_blocked,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Voum Admin',
    '+250780000000',
    'admin@voum.com',
    'ADMIN',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;
