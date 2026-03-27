package com.kafkaexample.iot.service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IngestionMetricsService {

    private static final Logger log = LoggerFactory.getLogger(IngestionMetricsService.class);

    private final LongAdder totalReceived = new LongAdder();
    private final LongAdder totalPersisted = new LongAdder();
    private final LongAdder totalInvalid = new LongAdder();
    private final LongAdder totalUnknown = new LongAdder();
    private final LongAdder batchCount = new LongAdder();
    private final LongAdder totalLatencyMillis = new LongAdder();
    private final AtomicLong lastWindowStartNanos = new AtomicLong(System.nanoTime());

    @Scheduled(
            fixedDelayString = "${iot.observability.log-interval-ms}",
            initialDelayString = "${iot.observability.log-interval-ms}")
    public void logWindow() {
        long batches = batchCount.sumThenReset();
        long received = totalReceived.sumThenReset();
        long persisted = totalPersisted.sumThenReset();
        long invalid = totalInvalid.sumThenReset();
        long unknown = totalUnknown.sumThenReset();
        long latencyMillis = totalLatencyMillis.sumThenReset();
        long now = System.nanoTime();
        long elapsedNanos = now - lastWindowStartNanos.getAndSet(now);

        if (batches == 0L && received == 0L) {
            return;
        }

        double elapsedSeconds = Math.max(elapsedNanos / 1_000_000_000.0d, 0.001d);
        double persistedPerSecond = persisted / elapsedSeconds;
        double averageLatencyMillis = batches == 0L ? 0.0d : latencyMillis / (double) batches;

        log.info(
                "Ingestion window: received={}, persisted={}, invalid={}, unknownDevices={}, batches={}, persistedRate={} msg/s, avgBatchLatencyMs={}",
                received,
                persisted,
                invalid,
                unknown,
                batches,
                String.format("%.2f", persistedPerSecond),
                String.format("%.2f", averageLatencyMillis));
    }

    public void recordBatch(int received, int persisted, int invalid, int unknown, Duration latency) {
        batchCount.increment();
        totalReceived.add(received);
        totalPersisted.add(persisted);
        totalInvalid.add(invalid);
        totalUnknown.add(unknown);
        totalLatencyMillis.add(latency.toMillis());
    }
}
