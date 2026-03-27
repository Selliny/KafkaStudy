package com.kafkaexample.iot.dto;

import java.time.Instant;

public record RejectedSensorEvent(
        String reason,
        String deviceId,
        String payload,
        String sourceTopic,
        int sourcePartition,
        long sourceOffset,
        Instant rejectedAt) {
}
