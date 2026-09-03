-- Create Motari Verification Profiles Table
CREATE TABLE IF NOT EXISTS motari_verification_profiles (
    motari_id UUID PRIMARY KEY,
    plate_number VARCHAR(50),
    national_id_number VARCHAR(50),
    permit_number VARCHAR(50),
    permit_expiry_date DATE,
    insurance_expiry_date DATE,
    verification_level VARCHAR(30) NOT NULL DEFAULT 'LEVEL_0',
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Verification Sessions Table
CREATE TABLE IF NOT EXISTS verification_sessions (
    id UUID PRIMARY KEY,
    motari_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    rejection_reason VARCHAR(255),
    admin_notes VARCHAR(1000),
    created_by UUID,
    updated_by UUID,
    verified_by UUID,
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITHOUT TIME ZONE,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Verification Audit Logs Table
CREATE TABLE IF NOT EXISTS verification_audit_logs (
    id UUID PRIMARY KEY,
    session_id UUID,
    document_id UUID,
    action VARCHAR(50) NOT NULL,
    performed_by UUID,
    metadata_json TEXT,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_verif_sessions_motari ON verification_sessions(motari_id);
CREATE INDEX IF NOT EXISTS idx_verif_sessions_status ON verification_sessions(status);
CREATE INDEX IF NOT EXISTS idx_verif_audit_logs_session ON verification_audit_logs(session_id);
