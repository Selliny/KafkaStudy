package com.kafkaexample.iot.service;

import com.kafkaexample.iot.dto.SensorDataEvent;
import com.kafkaexample.iot.entity.SensorMeasurementEntity;
import com.kafkaexample.iot.repository.SensorMeasurementRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensorEventPersistenceService {

    private final SensorMeasurementRepository sensorMeasurementRepository;

    public SensorEventPersistenceService(SensorMeasurementRepository sensorMeasurementRepository) {
        this.sensorMeasurementRepository = sensorMeasurementRepository;
    }

    @Transactional(timeout = 5)
    public void persistBatch(List<SensorDataEvent> events) {
        List<SensorMeasurementEntity> entities = events.stream()
                .map(this::toEntity)
                .toList();
        sensorMeasurementRepository.saveAll(entities);
    }

    private SensorMeasurementEntity toEntity(SensorDataEvent event) {
        SensorMeasurementEntity entity = new SensorMeasurementEntity();
        entity.setDeviceId(event.deviceId());
        entity.setSensorValue(event.value());
        entity.setEventTimestamp(event.timestamp());
        entity.setIngestedAt(Instant.now());
        return entity;
    }
}
