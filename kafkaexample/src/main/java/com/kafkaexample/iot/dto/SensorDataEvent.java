package com.kafkaexample.iot.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SensorDataEvent(String deviceId, BigDecimal value, Instant timestamp) {
}
