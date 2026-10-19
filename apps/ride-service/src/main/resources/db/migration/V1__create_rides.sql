CREATE TABLE rides (
    id UUID PRIMARY KEY,
    rider_id UUID NOT NULL,
    driver_id UUID,
    pickup_latitude DOUBLE PRECISION NOT NULL,
    pickup_longitude DOUBLE PRECISION NOT NULL,
    pickup_address TEXT,
    dropoff_latitude DOUBLE PRECISION NOT NULL,
    dropoff_longitude DOUBLE PRECISION NOT NULL,
    dropoff_address TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX rides_rider_id_idx ON rides(rider_id);
CREATE INDEX rides_status_idx ON rides(status);
