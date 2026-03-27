package com.kafkaexample.iot.consumer;

import com.kafkaexample.iot.config.IngestionProperties;
import com.kafkaexample.iot.dto.SensorDataEvent;
import com.kafkaexample.iot.service.IngestionMetricsService;
import com.kafkaexample.iot.service.ListenerBackpressureService;
import com.kafkaexample.iot.service.RejectedEventPublisher;
import com.kafkaexample.iot.service.SensorEventPersistenceService;
import com.kafkaexample.iot.service.SensorEventValidator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SensorDataConsumer {

    private static final Logger log = LoggerFactory.getLogger(SensorDataConsumer.class);

    private final SensorEventValidator sensorEventValidator;
    private final SensorEventPersistenceService persistenceService;
    private final RejectedEventPublisher rejectedEventPublisher;
    private final ListenerBackpressureService listenerBackpressureService;
    private final IngestionMetricsService ingestionMetricsService;
    private final IngestionProperties properties;

    public SensorDataConsumer(
            SensorEventValidator sensorEventValidator,
            SensorEventPersistenceService persistenceService,
            RejectedEventPublisher rejectedEventPublisher,
            ListenerBackpressureService listenerBackpressureService,
            IngestionMetricsService ingestionMetricsService,
            IngestionProperties properties) {
        this.sensorEventValidator = sensorEventValidator;
        this.persistenceService = persistenceService;
        this.rejectedEventPublisher = rejectedEventPublisher;
        this.listenerBackpressureService = listenerBackpressureService;
        this.ingestionMetricsService = ingestionMetricsService;
        this.properties = properties;
    }

    @KafkaListener(
            id = "${iot.consumer.listener-id}",
            topics = "${iot.topic.name}",
            containerFactory = "batchKafkaListenerContainerFactory")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        if (records.isEmpty()) {
            return;
        }

        if (listenerBackpressureService.isDatabaseUnderPressure()) {
            listenerBackpressureService.pauseListener(
                    properties.getConsumer().getListenerId(),
                    "Hikari pool usage exceeded threshold before batch persistence");
            throw new TransientDataAccessResourceException("TimescaleDB is under write pressure");
        }

        long startedAt = System.nanoTime();
        List<SensorDataEvent> acceptedEvents = new ArrayList<>(records.size());
        int invalidCount = 0;
        int unknownDeviceCount = 0;

        for (ConsumerRecord<String, String> record : records) {
            SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);
            switch (outcome.status()) {
                case ACCEPTED -> acceptedEvents.add(outcome.event());
                case INVALID -> {
                    invalidCount++;
                    rejectedEventPublisher.publishInvalid(record, outcome.deviceId(), outcome.reason());
                }
                case UNKNOWN_DEVICE -> {
                    unknownDeviceCount++;
                    rejectedEventPublisher.publishUnknownDevice(record, outcome.deviceId(), outcome.reason());
                }
            }
        }

        try {
            if (!acceptedEvents.isEmpty()) {
                persistenceService.persistBatch(acceptedEvents);
            }
        } catch (DataAccessException exception) {
            listenerBackpressureService.pauseListener(
                    properties.getConsumer().getListenerId(),
                    "TimescaleDB write exception: " + exception.getClass().getSimpleName());
            log.warn("Failed to persist batch of {} events", acceptedEvents.size(), exception);
            throw exception;
        }

        acknowledgment.acknowledge();
        ingestionMetricsService.recordBatch(
                records.size(),
                acceptedEvents.size(),
                invalidCount,
                unknownDeviceCount,
                Duration.ofNanos(System.nanoTime() - startedAt));
    }
}
