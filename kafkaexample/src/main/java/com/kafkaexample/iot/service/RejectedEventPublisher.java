package com.kafkaexample.iot.service;

// Excecao de serializacao do Jackson.
import com.fasterxml.jackson.core.JsonProcessingException;
// Mapper usado para transformar objetos em JSON.
import com.fasterxml.jackson.databind.ObjectMapper;
// Propriedades customizadas que guardam nomes de topicos.
import com.kafkaexample.iot.config.IngestionProperties;
// DTO do evento rejeitado.
import com.kafkaexample.iot.dto.RejectedSensorEvent;
import java.time.Duration;
// Tipo temporal usado para marcar o instante da rejeicao.
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
// Registro Kafka original que esta sendo rejeitado.
import org.apache.kafka.clients.consumer.ConsumerRecord;
// Logger SLF4J.
import org.slf4j.Logger;
// Factory do logger.
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
// Template Spring usado para publicar no Kafka.
import org.springframework.kafka.core.KafkaTemplate;
// Marca a classe como servico Spring.
import org.springframework.stereotype.Service;

// Publica eventos rejeitados em topicos auxiliares para analise posterior.
@Service
public class RejectedEventPublisher {

    // Tempo maximo aguardado para confirmacao de envio ao broker.
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    // Logger da classe.
    private static final Logger log = LoggerFactory.getLogger(RejectedEventPublisher.class);

    // Template Kafka usado para envio.
    private final KafkaTemplate<String, String> kafkaTemplate;
    // Mapper que converte o DTO de rejeicao em JSON.
    private final ObjectMapper objectMapper;
    // Propriedades com os nomes dos topicos auxiliares.
    private final IngestionProperties properties;

    // Construtor com injecao das dependencias.
    public RejectedEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            IngestionProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    // Publica um evento invalido no topico apropriado.
    public void publishInvalid(ConsumerRecord<String, String> record, String deviceId, String reason) {
        // Reaproveita o metodo interno de montagem e envio.
        publish(properties.getTopic().getInvalidName(), record, deviceId, reason);
    }

    // Publica um evento de device desconhecido no topico apropriado.
    public void publishUnknownDevice(ConsumerRecord<String, String> record, String deviceId, String reason) {
        // Reaproveita o metodo interno de montagem e envio.
        publish(properties.getTopic().getUnknownDeviceName(), record, deviceId, reason);
    }

    // Metodo interno que monta o envelope de rejeicao e envia ao Kafka.
    private void publish(
            String topic,
            ConsumerRecord<String, String> record,
            String deviceId,
            String reason) {
        try {
            // Converte o DTO de rejeicao em JSON.
            String payload = objectMapper.writeValueAsString(
                    new RejectedSensorEvent(
                            reason,
                            deviceId,
                            record.value(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            Instant.now()));
            // Aguarda confirmacao do broker para nao perder rejeicoes silenciosamente.
            kafkaTemplate.send(topic, deviceId == null ? "unknown-device" : deviceId, payload)
                    .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException exception) {
            // Registra erro de serializacao sem derrubar o processamento principal.
            log.error("Failed to serialize rejected event for topic {}", topic, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Interrupted while publishing rejected event", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new KafkaException("Failed to publish rejected event to topic " + topic, exception);
        }
    }
}
