package com.kafkaexample.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iot")
public class IngestionProperties {

    private final Topic topic = new Topic();
    private final Consumer consumer = new Consumer();
    private final Validation validation = new Validation();
    private final Backpressure backpressure = new Backpressure();
    private final Observability observability = new Observability();

    public Topic getTopic() {
        return topic;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public Validation getValidation() {
        return validation;
    }

    public Backpressure getBackpressure() {
        return backpressure;
    }

    public Observability getObservability() {
        return observability;
    }

    public static class Topic {
        private String name = "sensor_data";
        private String invalidName = "sensor_data_invalid";
        private String unknownDeviceName = "sensor_data_unknown_device";
        private int partitions = 12;
        private short replicationFactor = 1;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInvalidName() {
            return invalidName;
        }

        public void setInvalidName(String invalidName) {
            this.invalidName = invalidName;
        }

        public String getUnknownDeviceName() {
            return unknownDeviceName;
        }

        public void setUnknownDeviceName(String unknownDeviceName) {
            this.unknownDeviceName = unknownDeviceName;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    public static class Consumer {
        private String listenerId = "sensor-data-listener";
        private int concurrency = 6;
        private int maxPollRecords = 500;
        private int fetchMinBytes = 65536;
        private int fetchMaxWaitMs = 50;
        private int maxPartitionFetchBytes = 1048576;

        public String getListenerId() {
            return listenerId;
        }

        public void setListenerId(String listenerId) {
            this.listenerId = listenerId;
        }

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        public int getMaxPollRecords() {
            return maxPollRecords;
        }

        public void setMaxPollRecords(int maxPollRecords) {
            this.maxPollRecords = maxPollRecords;
        }

        public int getFetchMinBytes() {
            return fetchMinBytes;
        }

        public void setFetchMinBytes(int fetchMinBytes) {
            this.fetchMinBytes = fetchMinBytes;
        }

        public int getFetchMaxWaitMs() {
            return fetchMaxWaitMs;
        }

        public void setFetchMaxWaitMs(int fetchMaxWaitMs) {
            this.fetchMaxWaitMs = fetchMaxWaitMs;
        }

        public int getMaxPartitionFetchBytes() {
            return maxPartitionFetchBytes;
        }

        public void setMaxPartitionFetchBytes(int maxPartitionFetchBytes) {
            this.maxPartitionFetchBytes = maxPartitionFetchBytes;
        }
    }

    public static class Validation {
        private int maxDeviceIdLength = 64;
        private long deviceCacheRefreshMs = 60000;

        public int getMaxDeviceIdLength() {
            return maxDeviceIdLength;
        }

        public void setMaxDeviceIdLength(int maxDeviceIdLength) {
            this.maxDeviceIdLength = maxDeviceIdLength;
        }

        public long getDeviceCacheRefreshMs() {
            return deviceCacheRefreshMs;
        }

        public void setDeviceCacheRefreshMs(long deviceCacheRefreshMs) {
            this.deviceCacheRefreshMs = deviceCacheRefreshMs;
        }
    }

    public static class Backpressure {
        private double hikariUsageThreshold = 0.85d;
        private long pauseMs = 5000L;

        public double getHikariUsageThreshold() {
            return hikariUsageThreshold;
        }

        public void setHikariUsageThreshold(double hikariUsageThreshold) {
            this.hikariUsageThreshold = hikariUsageThreshold;
        }

        public long getPauseMs() {
            return pauseMs;
        }

        public void setPauseMs(long pauseMs) {
            this.pauseMs = pauseMs;
        }
    }

    public static class Observability {
        private long logIntervalMs = 10000L;

        public long getLogIntervalMs() {
            return logIntervalMs;
        }

        public void setLogIntervalMs(long logIntervalMs) {
            this.logIntervalMs = logIntervalMs;
        }
    }
}
