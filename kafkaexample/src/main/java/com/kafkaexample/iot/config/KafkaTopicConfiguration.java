package com.kafkaexample.iot.config;

// Tipo que representa um topico a ser criado pelo AdminClient.
import org.apache.kafka.clients.admin.NewTopic;
// Chaves de configuracao de topicos Kafka.
import org.apache.kafka.common.config.TopicConfig;
// Marca metodos que devolvem beans Spring.
import org.springframework.context.annotation.Bean;
// Marca a classe como configuracao Spring.
import org.springframework.context.annotation.Configuration;
// Builder utilitario para construcao de topicos.
import org.springframework.kafka.config.TopicBuilder;

// Configura os topicos Kafka usados pela aplicacao.
@Configuration
public class KafkaTopicConfiguration {

    // Cria o topico principal que recebe eventos validos.
    @Bean
    NewTopic sensorDataTopic(IngestionProperties properties) {
        // Constroi o topico principal usando os valores definidos em propriedades.
        return TopicBuilder.name(properties.getTopic().getName())
                // Define o numero de particoes.
                .partitions(properties.getTopic().getPartitions())
                // Define o fator de replicacao.
                .replicas(properties.getTopic().getReplicationFactor())
                // Define o numero minimo de replicas em sincronia para aceitar escrita.
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                // Mantem politica de limpeza por exclusao de segmentos antigos.
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                // Finaliza o objeto do topico.
                .build();
    }

    // Cria o topico para eventos com payload invalido.
    @Bean
    NewTopic invalidSensorDataTopic(IngestionProperties properties) {
        // Constroi o topico de invalidos com a mesma topologia basica do topico principal.
        return TopicBuilder.name(properties.getTopic().getInvalidName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    // Cria o topico para eventos de devices nao cadastrados.
    @Bean
    NewTopic unknownDeviceTopic(IngestionProperties properties) {
        // Constroi o topico de unknown device.
        return TopicBuilder.name(properties.getTopic().getUnknownDeviceName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    // Cria o topico de dead-letter para falhas permanentes do consumer principal.
    @Bean
    NewTopic sensorDataDeadLetterTopic(IngestionProperties properties) {
        return TopicBuilder.name(properties.getTopic().getDltName())
                .partitions(properties.getTopic().getPartitions())
                .replicas(properties.getTopic().getReplicationFactor())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "1")
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }
}
