package com.kafkaexample.iot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaexample.iot.config.IngestionProperties;
import com.kafkaexample.iot.dto.RejectedSensorEvent;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class RejectedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RejectedEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IngestionProperties properties;

    public RejectedEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            IngestionProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void publishInvalid(ConsumerRecord<String, String> record, String deviceId, String reason) {
        publish(properties.getTopic().getInvalidName(), record, deviceId, reason);
    }

    public void publishUnknownDevice(ConsumerRecord<String, String> record, String deviceId, String reason) {
        publish(properties.getTopic().getUnknownDeviceName(), record, deviceId, reason);
    }

    private void publish(
            String topic,
            ConsumerRecord<String, String> record,
            String deviceId,
            String reason) {
        try {
            String payload = objectMapper.writeValueAsString(
                    new RejectedSensorEvent(
                            reason,
                            deviceId,
                            record.value(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            Instant.now()));
            kafkaTemplate.send(topic, deviceId == null ? "unknown-device" : deviceId, payload);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize rejected event for topic {}", topic, exception);
        }
    }
}
