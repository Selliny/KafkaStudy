package com.kafkaexample.iot.service;

// Excecao usada quando o payload JSON nao pode ser desserializado.
import com.fasterxml.jackson.core.JsonProcessingException;
// Mapper Jackson para transformar JSON em DTO.
import com.fasterxml.jackson.databind.ObjectMapper;
// Propriedades com limites e parametros de validacao.
import com.kafkaexample.iot.config.IngestionProperties;
// DTO do evento quando o payload e valido.
import com.kafkaexample.iot.dto.SensorDataEvent;
// Registro bruto consumido do Kafka.
import org.apache.kafka.clients.consumer.ConsumerRecord;
// Marca a classe como servico Spring.
import org.springframework.stereotype.Service;
// Utilitarios para validar strings nulas ou vazias.
import org.springframework.util.StringUtils;

// Valida o contrato JSON e o cadastro de devices antes da persistencia.
@Service
public class SensorEventValidator {

    // Mapper usado para desserializar o JSON do Kafka.
    private final ObjectMapper objectMapper;
    // Cache local que informa se o device esta cadastrado.
    private final DeviceRegistryCache deviceRegistryCache;
    // Propriedades com limites de validacao.
    private final IngestionProperties properties;

    // Construtor com injecao das dependencias.
    public SensorEventValidator(
            ObjectMapper objectMapper,
            DeviceRegistryCache deviceRegistryCache,
            IngestionProperties properties) {
        this.objectMapper = objectMapper;
        this.deviceRegistryCache = deviceRegistryCache;
        this.properties = properties;
    }

    // Valida um registro do Kafka e devolve um resultado tipado.
    public ValidationOutcome validate(ConsumerRecord<String, String> record) {
        // Rejeita mensagens com payload vazio ou so com espacos.
        if (!StringUtils.hasText(record.value())) {
            return ValidationOutcome.invalid("empty_payload", record.key());
        }

        // Variavel que recebera o DTO desserializado.
        SensorDataEvent event;
        try {
            // Converte o JSON bruto em SensorDataEvent.
            event = objectMapper.readValue(record.value(), SensorDataEvent.class);
        } catch (JsonProcessingException exception) {
            // Se o JSON nao puder ser lido, classifica como invalido.
            return ValidationOutcome.invalid("malformed_json", record.key());
        }

        // Rejeita evento sem deviceId textual.
        if (!StringUtils.hasText(event.deviceId())) {
            return ValidationOutcome.invalid("missing_device_id", record.key());
        }

        // Remove espacos extras do deviceId recebido.
        String normalizedDeviceId = event.deviceId().trim();
        // Rejeita deviceId maior que o limite aceito.
        if (normalizedDeviceId.length() > properties.getValidation().getMaxDeviceIdLength()) {
            return ValidationOutcome.invalid("device_id_too_long", normalizedDeviceId);
        }

        // Rejeita payload sem valor numerico.
        if (event.value() == null) {
            return ValidationOutcome.invalid("missing_value", normalizedDeviceId);
        }

        // Rejeita payload sem timestamp.
        if (event.timestamp() == null) {
            return ValidationOutcome.invalid("missing_timestamp", normalizedDeviceId);
        }

        // Se a chave Kafka existir, ela precisa bater com o deviceId do payload.
        if (StringUtils.hasText(record.key()) && !normalizedDeviceId.equals(record.key())) {
            return ValidationOutcome.invalid("key_device_mismatch", normalizedDeviceId);
        }

        // Se o device nao estiver no cache de cadastrados, desvia para topico especifico.
        if (!deviceRegistryCache.isRegistered(normalizedDeviceId)) {
            return ValidationOutcome.unknownDevice(normalizedDeviceId, "device_not_registered");
        }

        // Retorna um DTO normalizado e aceito para persistencia.
        return ValidationOutcome.accepted(
                new SensorDataEvent(
                        normalizedDeviceId,
                        event.value(),
                        event.timestamp(),
                        record.topic(),
                        record.partition(),
                        record.offset()));
    }

    // Record que representa o resultado final da validacao.
    public record ValidationOutcome(
            // Status final da validacao.
            Status status,
            // Evento normalizado quando o payload e aceito.
            SensorDataEvent event,
            // Device relacionado ao evento.
            String deviceId,
            // Motivo tecnico da rejeicao, quando houver.
            String reason) {

        // Fabrica um resultado aceito.
        public static ValidationOutcome accepted(SensorDataEvent event) {
            return new ValidationOutcome(Status.ACCEPTED, event, event.deviceId(), null);
        }

        // Fabrica um resultado invalido.
        public static ValidationOutcome invalid(String reason, String deviceId) {
            return new ValidationOutcome(Status.INVALID, null, deviceId, reason);
        }

        // Fabrica um resultado de device desconhecido.
        public static ValidationOutcome unknownDevice(String deviceId, String reason) {
            return new ValidationOutcome(Status.UNKNOWN_DEVICE, null, deviceId, reason);
        }
    }

    // Enum que categoriza o desfecho da validacao.
    public enum Status {
        // Evento apto a seguir para persistencia.
        ACCEPTED,
        // Evento rejeitado por problema de contrato.
        INVALID,
        // Evento rejeitado porque o device nao esta cadastrado.
        UNKNOWN_DEVICE
    }
}
