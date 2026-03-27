package com.kafkaexample.iot.service;

import com.kafkaexample.iot.repository.DeviceRegistryRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DeviceRegistryCache {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryCache.class);

    private final DeviceRegistryRepository deviceRegistryRepository;
    private final Set<String> activeDevices = ConcurrentHashMap.newKeySet();

    public DeviceRegistryCache(DeviceRegistryRepository deviceRegistryRepository) {
        this.deviceRegistryRepository = deviceRegistryRepository;
    }

    @PostConstruct
    void loadOnStartup() {
        refresh();
    }

    @Scheduled(
            fixedDelayString = "${iot.validation.device-cache-refresh-ms}",
            initialDelayString = "0")
    public void refresh() {
        Set<String> latest = new HashSet<>(deviceRegistryRepository.findAllActiveDeviceIds());
        activeDevices.clear();
        activeDevices.addAll(latest);
        log.info("Device cache refreshed with {} active devices", activeDevices.size());
    }

    public boolean isRegistered(String deviceId) {
        return activeDevices.contains(deviceId);
    }
}
