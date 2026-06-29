-- Alter Users table to support status mapping
ALTER TABLE users ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'INACTIVE';

-- Alter Passengers table to support onboarding status
ALTER TABLE passengers ADD COLUMN onboarding_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS';

-- Alter Motaris table to support onboarding status and verification request reference
ALTER TABLE motaris ADD COLUMN onboarding_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS';
ALTER TABLE motaris ADD COLUMN verification_request_id UUID;

-- Create Uploaded Documents Table
CREATE TABLE uploaded_documents (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- "PROFILE_IMAGE", "NATIONAL_ID_FRONT", "NATIONAL_ID_BACK", "DRIVING_PERMIT", "MOTO_PHOTO"
    file_url VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Verification Requests Table (Admin workflow)
CREATE TABLE verification_requests (
    id UUID PRIMARY KEY,
    motari_id UUID NOT NULL REFERENCES motaris(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- "PENDING", "UNDER_REVIEW", "APPROVED", "REJECTED", "SUSPENDED"
    rejection_reason VARCHAR(255),
    admin_notes VARCHAR(1000),
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Indexes
CREATE INDEX idx_uploaded_documents_owner ON uploaded_documents(owner_id);
CREATE INDEX idx_verification_requests_motari ON verification_requests(motari_id);
CREATE INDEX idx_verification_requests_status ON verification_requests(status);
