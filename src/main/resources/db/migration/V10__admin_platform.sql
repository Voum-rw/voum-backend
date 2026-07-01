-- ============================================================
-- V10: Operations & Admin Management Platform Tables
-- ============================================================

-- 1. Extend Users Table
ALTER TABLE users ADD COLUMN suspension_reason VARCHAR(255) DEFAULT NULL;
ALTER TABLE users ADD COLUMN suspended_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NULL;
ALTER TABLE users ADD COLUMN suspended_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN deleted_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE users ADD COLUMN flag_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN is_flagged BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Create Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_phone VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    target VARCHAR(255),
    target_type VARCHAR(50),
    metadata TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- 3. Create Admin Notes Table
CREATE TABLE admin_notes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    note VARCHAR(2000) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_notes_user_id ON admin_notes(user_id);
CREATE INDEX idx_admin_notes_created_at ON admin_notes(created_at);
