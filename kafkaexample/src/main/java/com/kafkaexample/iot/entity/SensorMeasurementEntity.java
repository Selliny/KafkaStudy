package com.kafkaexample.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sensor_measurements")
public class SensorMeasurementEntity {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "sensor_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal sensorValue;

    @Column(name = "event_ts", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @PrePersist
    void prePersist() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public BigDecimal getSensorValue() {
        return sensorValue;
    }

    public void setSensorValue(BigDecimal sensorValue) {
        this.sensorValue = sensorValue;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
