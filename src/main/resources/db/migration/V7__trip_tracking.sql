-- ============================================================
-- V7: Live Trip Tracking and Location Synchronization
-- ============================================================

CREATE TABLE trip_tracking_points (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    sequence_number BIGINT NOT NULL,
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    accuracy DECIMAL(10,2) NOT NULL,
    speed_kmh DECIMAL(8,2) NOT NULL,
    heading_degrees DECIMAL(8,2) NOT NULL,
    battery_level INTEGER,
    gps_mocked BOOLEAN,
    recorded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add last location update timestamp to trips table for trust indicators
ALTER TABLE trips ADD COLUMN last_location_update_at TIMESTAMP WITHOUT TIME ZONE;

-- Performance indexes
CREATE INDEX idx_trip_tracking_points_trip_id ON trip_tracking_points(trip_id);
CREATE INDEX idx_trip_tracking_points_recorded_at ON trip_tracking_points(recorded_at);
CREATE UNIQUE INDEX idx_trip_tracking_points_trip_seq ON trip_tracking_points(trip_id, sequence_number);
