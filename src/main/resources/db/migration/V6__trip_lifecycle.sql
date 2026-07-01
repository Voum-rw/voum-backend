-- ============================================================
-- V6: Trip Lifecycle Table and Indexes
-- ============================================================

CREATE TABLE trips (
    id UUID PRIMARY KEY,
    trip_number BIGSERIAL UNIQUE NOT NULL,
    ride_request_id UUID NOT NULL REFERENCES ride_requests(id) ON DELETE RESTRICT,
    ride_offer_id UUID NOT NULL REFERENCES ride_offers(id) ON DELETE RESTRICT,
    passenger_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motari_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    pickup_address VARCHAR(255),
    
    destination_latitude DOUBLE PRECISION NOT NULL,
    destination_longitude DOUBLE PRECISION NOT NULL,
    destination_address VARCHAR(255),
    
    agreed_price DECIMAL(12,2) NOT NULL,
    estimated_arrival_minutes INTEGER,
    estimated_distance_km DECIMAL(8,2),
    
    status VARCHAR(30) NOT NULL,
    cancellation_reason VARCHAR(255),
    cancelled_by UUID REFERENCES users(id) ON DELETE SET NULL,
    
    current_latitude DOUBLE PRECISION,
    current_longitude DOUBLE PRECISION,
    
    last_status_change_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    cancelled_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_trips_passenger_id ON trips(passenger_id);
CREATE INDEX idx_trips_motari_id ON trips(motari_id);
CREATE INDEX idx_trips_status ON trips(status);
