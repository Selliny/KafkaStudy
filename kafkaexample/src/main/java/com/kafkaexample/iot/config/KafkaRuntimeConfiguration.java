package com.kafkaexample.iot.config;

// Implementacao mutavel de mapa usada para complementar propriedades.
import java.util.HashMap;
// Interface de mapa para propriedades de configuracao.
import java.util.Map;
// Constantes de configuracao do consumer Kafka.
import org.apache.kafka.clients.consumer.ConsumerConfig;
// Constantes de configuracao do producer Kafka.
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
// Deserializador padrao para chaves String.
import org.apache.kafka.common.serialization.StringDeserializer;
// Serializador padrao para chaves String.
import org.apache.kafka.common.serialization.StringSerializer;
// Propriedades Kafka auto-mapeadas pelo Spring Boot.
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
// Marca metodos que devolvem beans Spring.
import org.springframework.context.annotation.Bean;
// Marca a classe como configuracao Spring.
import org.springframework.context.annotation.Configuration;
// Factory de containers concorrentes para listeners Kafka.
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
// Contrato da fabrica de consumers.
import org.springframework.kafka.core.ConsumerFactory;
// Implementacao padrao da fabrica de consumers.
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
// Implementacao padrao da fabrica de producers.
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
// Publica registros em topicos de dead-letter apos esgotar retries.
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
// Template Spring para publicar no Kafka.
import org.springframework.kafka.core.KafkaTemplate;
// Contrato da fabrica de producers.
import org.springframework.kafka.core.ProducerFactory;
// Configuracoes do container do listener.
import org.springframework.kafka.listener.ContainerProperties;
// Error handler padrao do Spring Kafka.
import org.springframework.kafka.listener.DefaultErrorHandler;
// Scheduler usado para agendar retomada de listeners pausados.
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
// Estrategia de retry com intervalo fixo.
import org.springframework.util.backoff.FixedBackOff;

// Reune os beans de runtime ligados a consumo e producao Kafka.
@Configuration
public class KafkaRuntimeConfiguration {

    // Cria a factory de consumers usando as propriedades base do Spring Boot.
    @Bean
    ConsumerFactory<String, String> consumerFactory(
            KafkaProperties kafkaProperties,
            IngestionProperties ingestionProperties) {
        // Copia as propriedades geradas automaticamente pelo Spring Boot.
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
        // Garante que as chaves chegarao como String.
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Garante que os valores chegarao como String JSON bruta.
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Desliga commit automatico para o offset so ser confirmado apos persistencia.
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Ajusta o numero maximo de registros devolvidos por poll.
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, ingestionProperties.getConsumer().getMaxPollRecords());
        // Define um fetch minimo para reduzir overhead de requests muito pequenos.
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, ingestionProperties.getConsumer().getFetchMinBytes());
        // Define o tempo maximo de espera do broker para completar o fetch.
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, ingestionProperties.getConsumer().getFetchMaxWaitMs());
        // Define o teto de dados puxados por particao em cada fetch.
        config.put(
                ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
                ingestionProperties.getConsumer().getMaxPartitionFetchBytes());
        // Usa assignor cooperativo para reduzir rebalance agressivo entre consumidores.
        config.put(
                ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");
        // Devolve a factory final usada pelo listener Kafka.
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // Cria a factory de producers usada para publicar em topicos de rejeicao.
    @Bean
    ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
        // Copia as propriedades base de producer calculadas pelo Spring Boot.
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        // Garante serializacao de chave em String.
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Garante serializacao de valor em String.
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Devolve a factory do producer.
        return new DefaultKafkaProducerFactory<>(config);
    }

    // Expoe um KafkaTemplate para envio a partir dos servicos da aplicacao.
    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        // Instancia o template com a factory de producer configurada.
        return new KafkaTemplate<>(producerFactory);
    }

    // Define a politica padrao de retry para excecoes do listener.
    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            IngestionProperties ingestionProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        ingestionProperties.getTopic().getDltName(),
                        record.partition()));
        // Tenta reprocessar quatro vezes, com um segundo entre as tentativas.
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 4L));
        // Mantem o ack pendente para o offset nao ser confirmado apos tratamento de erro.
        errorHandler.setAckAfterHandle(false);
        // Retorna o handler final.
        return errorHandler;
    }

    // Configura a factory dos listeners em modo batch.
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> batchKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler,
            IngestionProperties ingestionProperties) {
        // Cria a factory responsavel por montar os containers do listener.
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        // Injeta a consumer factory customizada.
        factory.setConsumerFactory(consumerFactory);
        // Ativa o consumo em lote para melhorar throughput.
        factory.setBatchListener(true);
        // Define a concorrencia do listener a partir da configuracao.
        factory.setConcurrency(ingestionProperties.getConsumer().getConcurrency());
        // Registra o error handler padrao.
        factory.setCommonErrorHandler(kafkaErrorHandler);
        // Usa ack manual para confirmar offsets so depois do processamento.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        // Habilita observacao para integracao com metricas e tracing do Spring.
        factory.getContainerProperties().setObservationEnabled(true);
        // Retorna a factory pronta.
        return factory;
    }

    // Cria o scheduler usado pelo servico de backpressure.
    @Bean
    ThreadPoolTaskScheduler backpressureTaskScheduler() {
        // Instancia o scheduler.
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Duas threads bastam para agendar retomadas de listeners pausados.
        scheduler.setPoolSize(2);
        // Define um prefixo amigavel para os nomes das threads.
        scheduler.setThreadNamePrefix("backpressure-");
        // Inicializa o scheduler antes do uso.
        scheduler.initialize();
        // Devolve o scheduler pronto.
        return scheduler;
    }
}
