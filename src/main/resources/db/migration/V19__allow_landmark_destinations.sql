-- Landmark-only destinations have no invented coordinates.
ALTER TABLE ride_requests ALTER COLUMN destination_latitude DROP NOT NULL;
ALTER TABLE ride_requests ALTER COLUMN destination_longitude DROP NOT NULL;
ALTER TABLE trips ALTER COLUMN destination_latitude DROP NOT NULL;
ALTER TABLE trips ALTER COLUMN destination_longitude DROP NOT NULL;
ALTER TABLE ride_requests ADD CONSTRAINT request_destination_pair CHECK ((destination_latitude IS NULL) = (destination_longitude IS NULL));
ALTER TABLE trips ADD CONSTRAINT trip_destination_pair CHECK ((destination_latitude IS NULL) = (destination_longitude IS NULL));
