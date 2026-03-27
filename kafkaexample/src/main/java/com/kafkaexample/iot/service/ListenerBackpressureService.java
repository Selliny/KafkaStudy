package com.kafkaexample.iot.service;

import com.kafkaexample.iot.config.IngestionProperties;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class ListenerBackpressureService {

    private static final Logger log = LoggerFactory.getLogger(ListenerBackpressureService.class);

    private final KafkaListenerEndpointRegistry registry;
    private final ThreadPoolTaskScheduler scheduler;
    private final HikariDataSource hikariDataSource;
    private final IngestionProperties properties;
    private final ConcurrentHashMap<String, AtomicBoolean> pauseGuards = new ConcurrentHashMap<>();

    public ListenerBackpressureService(
            KafkaListenerEndpointRegistry registry,
            ThreadPoolTaskScheduler scheduler,
            HikariDataSource hikariDataSource,
            IngestionProperties properties) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.hikariDataSource = hikariDataSource;
        this.properties = properties;
    }

    public boolean isDatabaseUnderPressure() {
        HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
        if (pool == null) {
            return false;
        }

        int denominator = Math.max(pool.getTotalConnections(), hikariDataSource.getMaximumPoolSize());
        if (denominator == 0) {
            return false;
        }

        double usage = pool.getActiveConnections() / (double) denominator;
        return usage >= properties.getBackpressure().getHikariUsageThreshold()
                || pool.getThreadsAwaitingConnection() > 0;
    }

    public void pauseListener(String listenerId, String reason) {
        AtomicBoolean guard = pauseGuards.computeIfAbsent(listenerId, ignored -> new AtomicBoolean(false));
        if (!guard.compareAndSet(false, true)) {
            return;
        }

        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            guard.set(false);
            return;
        }

        container.pause();
        log.warn("Kafka listener {} paused due to {}", listenerId, reason);

        scheduler.schedule(
                () -> {
                    try {
                        container.resume();
                        log.info("Kafka listener {} resumed after backpressure cooldown", listenerId);
                    } finally {
                        guard.set(false);
                    }
                },
                Instant.now().plusMillis(properties.getBackpressure().getPauseMs()));
    }
}
