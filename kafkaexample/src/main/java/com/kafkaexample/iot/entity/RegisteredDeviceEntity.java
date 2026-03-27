package com.kafkaexample.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "device_registry")
public class RegisteredDeviceEntity {

    @Id
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }
}
