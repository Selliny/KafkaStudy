package com.kafkaexample.iot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kafkaexample.iot.config.IngestionProperties;
import com.kafkaexample.iot.service.DeviceRegistryCache;
import com.kafkaexample.iot.service.SensorEventValidator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SensorEventValidatorTest {

    private DeviceRegistryCache deviceRegistryCache;
    private SensorEventValidator sensorEventValidator;

    @BeforeEach
    void setUp() {
        deviceRegistryCache = Mockito.mock(DeviceRegistryCache.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        sensorEventValidator = new SensorEventValidator(objectMapper, deviceRegistryCache, new IngestionProperties());
    }

    @Test
    void shouldAcceptRegisteredDeviceWithValidPayload() {
        when(deviceRegistryCache.isRegistered("device-0001")).thenReturn(true);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "sensor_data",
                0,
                1L,
                "device-0001",
                """
                {"deviceId":"device-0001","value":21.45,"timestamp":"2026-03-27T15:00:00Z"}
                """);

        SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);

        assertThat(outcome.status()).isEqualTo(SensorEventValidator.Status.ACCEPTED);
        assertThat(outcome.event()).isNotNull();
        assertThat(outcome.event().deviceId()).isEqualTo("device-0001");
    }

    @Test
    void shouldRejectUnknownDevice() {
        when(deviceRegistryCache.isRegistered("device-9999")).thenReturn(false);

        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "sensor_data",
                0,
                1L,
                "device-9999",
                """
                {"deviceId":"device-9999","value":21.45,"timestamp":"2026-03-27T15:00:00Z"}
                """);

        SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);

        assertThat(outcome.status()).isEqualTo(SensorEventValidator.Status.UNKNOWN_DEVICE);
        assertThat(outcome.reason()).isEqualTo("device_not_registered");
    }
}
