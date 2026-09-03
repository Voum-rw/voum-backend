-- ============================================================
-- V16: Seed Primary Super Admin User (Joseph Manizabayo)
-- ============================================================

INSERT INTO users (
    id,
    name,
    phone,
    email,
    password,
    role,
    is_verified,
    is_blocked,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Joseph Manizabayo',
    '+250788000111',
    'josephmanizabayo7@gmail.com',
    '$2a$10$3zR1.L1u6r7Xq0dO9gE.v.l7e1E4XqM2wV3yN4r5s6t7u8v9w0x1y',
    'SUPER_ADMIN',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE 
SET name = 'Joseph Manizabayo',
    role = 'SUPER_ADMIN',
    is_verified = TRUE,
    is_blocked = FALSE;
