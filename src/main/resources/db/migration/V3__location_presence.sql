CREATE TABLE user_locations (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    accuracy DECIMAL(10,2),
    availability_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE', -- "ONLINE", "OFFLINE", "BUSY"
    last_seen_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_locations_user ON user_locations(user_id);
CREATE INDEX idx_user_locations_status ON user_locations(availability_status);
CREATE INDEX idx_user_locations_lat ON user_locations(latitude);
CREATE INDEX idx_user_locations_lng ON user_locations(longitude);
CREATE INDEX idx_user_locations_last_seen ON user_locations(last_seen_at);
