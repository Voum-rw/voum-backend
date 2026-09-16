ALTER TABLE trips ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE trips ADD COLUMN completion_requested_by UUID REFERENCES users(id);
ALTER TABLE trips ADD COLUMN completion_requested_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE trips ADD COLUMN completion_confirmed_by UUID REFERENCES users(id);
