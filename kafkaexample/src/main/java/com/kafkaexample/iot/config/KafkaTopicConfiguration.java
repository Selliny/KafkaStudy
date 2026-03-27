package com.kafkaexample.iot.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {

    @Bean
    NewTopic sensorDataTopic(IngestionProperties properties) {
        return TopicBuilder.name(properties.getTopic().getName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    NewTopic invalidSensorDataTopic(IngestionProperties properties) {
        return TopicBuilder.name(properties.getTopic().getInvalidName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    NewTopic unknownDeviceTopic(IngestionProperties properties) {
        return TopicBuilder.name(properties.getTopic().getUnknownDeviceName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
