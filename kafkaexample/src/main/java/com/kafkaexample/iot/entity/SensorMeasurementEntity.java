package com.kafkaexample.iot.entity;

// Mapeia colunas simples do banco.
import jakarta.persistence.Column;
// Marca a classe como entidade JPA.
import jakarta.persistence.Entity;
// Marca o campo de chave primaria.
import jakarta.persistence.Id;
// Hook executado antes do insert.
import jakarta.persistence.PrePersist;
// Define a tabela mapeada.
import jakarta.persistence.Table;
// Tipo decimal usado para o valor do sensor.
import java.math.BigDecimal;
// Tipo temporal usado nos timestamps.
import java.time.Instant;
// UUID usado como chave tecnica do evento.
import java.util.UUID;

// Entidade JPA correspondente a tabela temporal de medicoes.
@Entity
@Table(name = "sensor_measurements")
public class SensorMeasurementEntity {

    // Identificador tecnico unico do evento.
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    // Identificador do dispositivo dono da medicao.
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    // Valor numerico medido pelo sensor.
    @Column(name = "sensor_value", nullable = false, precision = 18, scale = 6)
    private BigDecimal sensorValue;

    // Timestamp original do evento vindo do produtor.
    @Column(name = "event_ts", nullable = false)
    private Instant eventTimestamp;

    // Topico Kafka de origem do evento.
    @Column(name = "source_topic", nullable = false, length = 255)
    private String sourceTopic;

    // Particao Kafka de origem do evento.
    @Column(name = "source_partition", nullable = false)
    private int sourcePartition;

    // Offset Kafka de origem do evento.
    @Column(name = "source_offset", nullable = false)
    private long sourceOffset;

    // Momento em que o backend gravou a medicao.
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    // Preenche campos tecnicos automaticamente antes do insert.
    @PrePersist
    void prePersist() {
        // Se nao houver UUID definido, gera um novo.
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        // Se nao houver horario de ingestao, usa o instante atual.
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }

    // Retorna o UUID do evento.
    public UUID getEventId() {
        return eventId;
    }

    // Define o UUID do evento.
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    // Retorna o id do device.
    public String getDeviceId() {
        return deviceId;
    }

    // Define o id do device.
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    // Retorna o valor do sensor.
    public BigDecimal getSensorValue() {
        return sensorValue;
    }

    // Define o valor do sensor.
    public void setSensorValue(BigDecimal sensorValue) {
        this.sensorValue = sensorValue;
    }

    // Retorna o timestamp original do evento.
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    // Define o timestamp original do evento.
    public void setEventTimestamp(Instant eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    // Retorna o topico Kafka de origem.
    public String getSourceTopic() {
        return sourceTopic;
    }

    // Define o topico Kafka de origem.
    public void setSourceTopic(String sourceTopic) {
        this.sourceTopic = sourceTopic;
    }

    // Retorna a particao Kafka de origem.
    public int getSourcePartition() {
        return sourcePartition;
    }

    // Define a particao Kafka de origem.
    public void setSourcePartition(int sourcePartition) {
        this.sourcePartition = sourcePartition;
    }

    // Retorna o offset Kafka de origem.
    public long getSourceOffset() {
        return sourceOffset;
    }

    // Define o offset Kafka de origem.
    public void setSourceOffset(long sourceOffset) {
        this.sourceOffset = sourceOffset;
    }

    // Retorna o horario em que a aplicacao ingeriu o evento.
    public Instant getIngestedAt() {
        return ingestedAt;
    }

    // Define o horario em que a aplicacao ingeriu o evento.
    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
