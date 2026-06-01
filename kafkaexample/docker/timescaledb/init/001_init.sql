-- Garante que a extensao TimescaleDB esteja disponivel no banco.
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Cria a tabela de cadastro dos devices conhecidos pelo sistema.
CREATE TABLE IF NOT EXISTS device_registry (
    -- Identificador logico do dispositivo.
    device_id VARCHAR(64) PRIMARY KEY,
    -- Flag que indica se o device esta ativo para ingestao.
    active BOOLEAN NOT NULL DEFAULT TRUE,
    -- Momento em que o cadastro foi criado.
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Faz um bootstrap de 5.000 devices para testes de carga.
INSERT INTO device_registry (device_id, active)
SELECT 'device-' || LPAD(gs::text, 4, '0'), TRUE
FROM generate_series(1, 5000) AS gs
ON CONFLICT (device_id) DO NOTHING;

-- Cria a tabela base das medicoes de sensores.
CREATE TABLE IF NOT EXISTS sensor_measurements (
    -- Chave tecnica unica do evento.
    event_id UUID NOT NULL,
    -- Device dono da medicao, referenciando o cadastro.
    device_id VARCHAR(64) NOT NULL REFERENCES device_registry(device_id),
    -- Valor numerico do sensor.
    sensor_value NUMERIC(18, 6) NOT NULL,
    -- Timestamp original do evento.
    event_ts TIMESTAMPTZ NOT NULL,
    -- Topico Kafka de origem do evento.
    source_topic VARCHAR(255) NOT NULL,
    -- Particao Kafka de origem do evento.
    source_partition INTEGER NOT NULL,
    -- Offset Kafka de origem do evento.
    source_offset BIGINT NOT NULL,
    -- Momento em que a aplicacao ingeriu o evento.
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE sensor_measurements
    ADD COLUMN IF NOT EXISTS source_topic VARCHAR(255);

ALTER TABLE sensor_measurements
    ADD COLUMN IF NOT EXISTS source_partition INTEGER;

ALTER TABLE sensor_measurements
    ADD COLUMN IF NOT EXISTS source_offset BIGINT;

-- Converte a tabela em hypertable particionada por tempo.
SELECT create_hypertable(
    'sensor_measurements',
    'event_ts',
    if_not_exists => TRUE,
    chunk_time_interval => INTERVAL '1 day'
);

-- Cria indice util para consultas por device ordenadas por tempo.
CREATE INDEX IF NOT EXISTS idx_sensor_measurements_device_ts
    ON sensor_measurements (device_id, event_ts DESC);

-- Cria indice util para consultas globais por tempo.
CREATE INDEX IF NOT EXISTS idx_sensor_measurements_event_ts
    ON sensor_measurements (event_ts DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sensor_measurements_source_record
    ON sensor_measurements (source_topic, source_partition, source_offset);

-- Remove a politica antiga de retencao, se existir.
SELECT remove_retention_policy('sensor_measurements', if_exists => TRUE);
-- Cria a politica que mantem apenas 30 dias de dados.
SELECT add_retention_policy('sensor_measurements', INTERVAL '30 days', if_not_exists => TRUE);
