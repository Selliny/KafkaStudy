CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS device_registry (
    device_id VARCHAR(64) PRIMARY KEY,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO device_registry (device_id, active)
SELECT 'device-' || LPAD(gs::text, 4, '0'), TRUE
FROM generate_series(1, 5000) AS gs
ON CONFLICT (device_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS sensor_measurements (
    event_id UUID NOT NULL,
    device_id VARCHAR(64) NOT NULL REFERENCES device_registry(device_id),
    sensor_value NUMERIC(18, 6) NOT NULL,
    event_ts TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable(
    'sensor_measurements',
    'event_ts',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '1 day'
);

CREATE INDEX IF NOT EXISTS idx_sensor_measurements_device_ts
    ON sensor_measurements (device_id, event_ts DESC);

CREATE INDEX IF NOT EXISTS idx_sensor_measurements_event_ts
    ON sensor_measurements (event_ts DESC);

SELECT remove_retention_policy('sensor_measurements', if_exists => TRUE);
SELECT add_retention_policy('sensor_measurements', INTERVAL '30 days', if_not_exists => TRUE);
