CREATE TABLE ride_requests (
    id UUID PRIMARY KEY,
    passenger_id UUID NOT NULL REFERENCES passengers(id) ON DELETE CASCADE,
    pickup_latitude DECIMAL(10,8) NOT NULL,
    pickup_longitude DECIMAL(11,8) NOT NULL,
    destination_latitude DECIMAL(10,8) NOT NULL,
    destination_longitude DECIMAL(11,8) NOT NULL,
    pickup_address VARCHAR(255) NOT NULL,
    destination_address VARCHAR(255) NOT NULL,
    proposed_budget DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- "OPEN", "EXPIRED", "MATCHED", "CANCELLED"
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    selected_offer_id UUID,
    offers_count INTEGER NOT NULL DEFAULT 0,
    request_version INTEGER NOT NULL DEFAULT 1,
    visibility_radius_km DECIMAL(5,2) NOT NULL DEFAULT 3.00,
    created_area VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ride_offers (
    id UUID PRIMARY KEY,
    ride_request_id UUID NOT NULL REFERENCES ride_requests(id) ON DELETE CASCADE,
    motari_id UUID NOT NULL REFERENCES motaris(id) ON DELETE CASCADE,
    offered_price DECIMAL(12,2) NOT NULL,
    estimated_arrival_minutes INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- "PENDING", "ACCEPTED", "REJECTED", "WITHDRAWN"
    update_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ride_requests_passenger ON ride_requests(passenger_id);
CREATE INDEX idx_ride_requests_status ON ride_requests(status);
CREATE INDEX idx_ride_offers_request ON ride_offers(ride_request_id);
CREATE INDEX idx_ride_offers_motari ON ride_offers(motari_id);
CREATE INDEX idx_ride_offers_status ON ride_offers(status);
