-- ============================================================
-- V9: Ratings, Reviews & Trust System
-- ============================================================

-- 1. Extend Motaris Table
ALTER TABLE motaris ADD COLUMN average_rating DECIMAL(3,2) DEFAULT NULL;
ALTER TABLE motaris ADD COLUMN total_reviews INTEGER NOT NULL DEFAULT 0;
ALTER TABLE motaris ADD COLUMN total_completed_trips INTEGER NOT NULL DEFAULT 0;
ALTER TABLE motaris ADD COLUMN completion_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00;
ALTER TABLE motaris ADD COLUMN acceptance_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00;
ALTER TABLE motaris ADD COLUMN cancellation_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00;
ALTER TABLE motaris ADD COLUMN trust_score DECIMAL(5,2) NOT NULL DEFAULT 50.00;

-- 2. Extend Passengers Table
ALTER TABLE passengers ADD COLUMN average_rating DECIMAL(3,2) DEFAULT NULL;
ALTER TABLE passengers ADD COLUMN total_reviews INTEGER NOT NULL DEFAULT 0;
ALTER TABLE passengers ADD COLUMN cancellation_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00;

-- 3. Create Trip Reviews Table (Separate rows for reviewer autonomy)
CREATE TABLE trip_reviews (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewed_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL,
    comment VARCHAR(1000),
    is_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flag_reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT uq_trip_reviewer UNIQUE (trip_id, reviewer_id)
);

-- Indexes for performance
CREATE INDEX idx_trip_reviews_trip ON trip_reviews(trip_id);
CREATE INDEX idx_trip_reviews_reviewer ON trip_reviews(reviewer_id);
CREATE INDEX idx_trip_reviews_reviewed ON trip_reviews(reviewed_user_id);
