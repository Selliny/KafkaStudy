package com.kafkaexample.iot.repository;

import com.kafkaexample.iot.entity.SensorMeasurementEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorMeasurementRepository extends JpaRepository<SensorMeasurementEntity, UUID> {
}
