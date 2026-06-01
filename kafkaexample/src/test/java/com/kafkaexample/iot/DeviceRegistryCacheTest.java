package com.kafkaexample.iot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kafkaexample.iot.repository.DeviceRegistryRepository;
import com.kafkaexample.iot.service.DeviceRegistryCache;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DeviceRegistryCacheTest {

    @Test
    void shouldSwapCacheSnapshotAtomicallyOnRefresh() {
        DeviceRegistryRepository repository = Mockito.mock(DeviceRegistryRepository.class);
        when(repository.findAllActiveDeviceIds())
                .thenReturn(List.of("device-0001", "device-0002"))
                .thenReturn(List.of("device-0003"));

        DeviceRegistryCache cache = new DeviceRegistryCache(repository);

        cache.refresh();
        assertThat(cache.isRegistered("device-0001")).isTrue();
        assertThat(cache.isRegistered("device-0003")).isFalse();

        cache.refresh();
        assertThat(cache.isRegistered("device-0001")).isFalse();
        assertThat(cache.isRegistered("device-0003")).isTrue();
    }
}
