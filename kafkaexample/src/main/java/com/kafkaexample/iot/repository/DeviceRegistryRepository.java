package com.kafkaexample.iot.repository;

import com.kafkaexample.iot.entity.RegisteredDeviceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRegistryRepository extends JpaRepository<RegisteredDeviceEntity, String> {

    @Query("select d.deviceId from RegisteredDeviceEntity d where d.active = true")
    List<String> findAllActiveDeviceIds();
}
