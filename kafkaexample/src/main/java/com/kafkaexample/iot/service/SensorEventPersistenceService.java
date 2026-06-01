package com.kafkaexample.iot.service;

// DTO usado como entrada da persistencia.
import com.kafkaexample.iot.dto.SensorDataEvent;
// Charset padrao usado na derivacao deterministica do UUID.
import java.nio.charset.StandardCharsets;
// Tipo temporal usado para carimbar a ingestao.
import java.time.Instant;
// Lista de eventos do lote.
import java.util.List;
// UUID tecnico derivado do metadado Kafka.
import java.util.UUID;
// Marca a classe como servico Spring.
import org.springframework.jdbc.core.JdbcTemplate;
// Marca a classe como servico Spring.
import org.springframework.stereotype.Service;
// Delimita a transacao de banco.
import org.springframework.transaction.annotation.Transactional;

// Persiste eventos aceitos em lote com idempotencia baseada em metadado Kafka.
@Service
public class SensorEventPersistenceService {

    // SQL de insert com deduplicacao por topico, particao e offset.
    private static final String INSERT_SQL = """
            INSERT INTO sensor_measurements (
                event_id,
                device_id,
                sensor_value,
                event_ts,
                source_topic,
                source_partition,
                source_offset,
                ingested_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (source_topic, source_partition, source_offset) DO NOTHING
            """;

    // Template JDBC usado para batch insert explicito.
    private final JdbcTemplate jdbcTemplate;

    // Construtor com injecao do template JDBC.
    public SensorEventPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Persiste um lote de eventos em uma unica transacao.
    @Transactional(timeout = 5)
    public void persistBatch(List<SensorDataEvent> events) {
        Instant ingestedAt = Instant.now();
        jdbcTemplate.batchUpdate(
                INSERT_SQL,
                events,
                events.size(),
                (preparedStatement, event) -> {
                    preparedStatement.setObject(1, deterministicEventId(event));
                    preparedStatement.setString(2, event.deviceId());
                    preparedStatement.setBigDecimal(3, event.value());
                    preparedStatement.setObject(4, event.timestamp());
                    preparedStatement.setString(5, event.sourceTopic());
                    preparedStatement.setInt(6, event.sourcePartition());
                    preparedStatement.setLong(7, event.sourceOffset());
                    preparedStatement.setObject(8, ingestedAt);
                });
    }

    // Deriva o mesmo UUID sempre que o Kafka reentregar o mesmo registro.
    private UUID deterministicEventId(SensorDataEvent event) {
        String seed = event.sourceTopic() + ":" + event.sourcePartition() + ":" + event.sourceOffset();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
