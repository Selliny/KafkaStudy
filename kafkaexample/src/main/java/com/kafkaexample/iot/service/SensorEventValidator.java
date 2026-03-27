package com.kafkaexample.iot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaexample.iot.config.IngestionProperties;
import com.kafkaexample.iot.dto.SensorDataEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SensorEventValidator {

    private final ObjectMapper objectMapper;
    private final DeviceRegistryCache deviceRegistryCache;
    private final IngestionProperties properties;

    public SensorEventValidator(
            ObjectMapper objectMapper,
            DeviceRegistryCache deviceRegistryCache,
            IngestionProperties properties) {
        this.objectMapper = objectMapper;
        this.deviceRegistryCache = deviceRegistryCache;
        this.properties = properties;
    }

    public ValidationOutcome validate(ConsumerRecord<String, String> record) {
        if (!StringUtils.hasText(record.value())) {
            return ValidationOutcome.invalid("empty_payload", record.key());
        }

        SensorDataEvent event;
        try {
            event = objectMapper.readValue(record.value(), SensorDataEvent.class);
        } catch (JsonProcessingException exception) {
            return ValidationOutcome.invalid("malformed_json", record.key());
        }

        if (!StringUtils.hasText(event.deviceId())) {
            return ValidationOutcome.invalid("missing_device_id", record.key());
        }

        String normalizedDeviceId = event.deviceId().trim();
        if (normalizedDeviceId.length() > properties.getValidation().getMaxDeviceIdLength()) {
            return ValidationOutcome.invalid("device_id_too_long", normalizedDeviceId);
        }

        if (event.value() == null) {
            return ValidationOutcome.invalid("missing_value", normalizedDeviceId);
        }

        if (event.timestamp() == null) {
            return ValidationOutcome.invalid("missing_timestamp", normalizedDeviceId);
        }

        if (StringUtils.hasText(record.key()) && !normalizedDeviceId.equals(record.key())) {
            return ValidationOutcome.invalid("key_device_mismatch", normalizedDeviceId);
        }

        if (!deviceRegistryCache.isRegistered(normalizedDeviceId)) {
            return ValidationOutcome.unknownDevice(normalizedDeviceId, "device_not_registered");
        }

        return ValidationOutcome.accepted(
                new SensorDataEvent(normalizedDeviceId, event.value(), event.timestamp()));
    }

    public record ValidationOutcome(
            Status status,
            SensorDataEvent event,
            String deviceId,
            String reason) {

        public static ValidationOutcome accepted(SensorDataEvent event) {
            return new ValidationOutcome(Status.ACCEPTED, event, event.deviceId(), null);
        }

        public static ValidationOutcome invalid(String reason, String deviceId) {
            return new ValidationOutcome(Status.INVALID, null, deviceId, reason);
        }

        public static ValidationOutcome unknownDevice(String deviceId, String reason) {
            return new ValidationOutcome(Status.UNKNOWN_DEVICE, null, deviceId, reason);
        }
    }

    public enum Status {
        ACCEPTED,
        INVALID,
        UNKNOWN_DEVICE
    }
}
