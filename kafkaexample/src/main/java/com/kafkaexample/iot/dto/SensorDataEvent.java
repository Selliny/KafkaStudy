package com.kafkaexample.iot.dto;

// Tipo decimal apropriado para valores numericos de sensor.
import java.math.BigDecimal;
// Tipo temporal usado no timestamp do evento.
import java.time.Instant;

// Record imutavel que representa o payload valido do evento de sensor.
public record SensorDataEvent(
        // Identificador do dispositivo.
        String deviceId,
        // Valor numerico do sensor.
        BigDecimal value,
        // Timestamp original do evento.
        Instant timestamp,
        // Topico Kafka de origem do evento.
        String sourceTopic,
        // Particao Kafka de origem do evento.
        int sourcePartition,
        // Offset Kafka de origem do evento.
        long sourceOffset) {
}
