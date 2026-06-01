package com.kafkaexample.iot.consumer;

// Propriedades customizadas do projeto.
import com.kafkaexample.iot.config.IngestionProperties;
// DTO do evento aceito para persistencia.
import com.kafkaexample.iot.dto.SensorDataEvent;
// Servico que agrega metricas do pipeline.
import com.kafkaexample.iot.service.IngestionMetricsService;
// Servico responsavel por pausar e retomar o listener sob pressao.
import com.kafkaexample.iot.service.ListenerBackpressureService;
// Servico que envia eventos rejeitados para topicos auxiliares.
import com.kafkaexample.iot.service.RejectedEventPublisher;
// Servico que grava os eventos aceitos no banco.
import com.kafkaexample.iot.service.SensorEventPersistenceService;
// Servico que valida payloads recebidos do Kafka.
import com.kafkaexample.iot.service.SensorEventValidator;
// Tipo temporal usado para medir latencia do lote.
import java.time.Duration;
// Lista dinamica para acumular eventos aceitos.
import java.util.ArrayList;
// Interface de lista usada no lote recebido.
import java.util.List;
// Representa cada registro consumido do Kafka.
import org.apache.kafka.clients.consumer.ConsumerRecord;
// Logger SLF4J.
import org.slf4j.Logger;
// Factory do logger.
import org.slf4j.LoggerFactory;
// Excecao base de acesso a dados.
import org.springframework.dao.DataAccessException;
// Excecao usada para sinalizar falha transitoria de recurso.
import org.springframework.dao.TransientDataAccessResourceException;
// Anotacao que registra o metodo como listener Kafka.
import org.springframework.kafka.annotation.KafkaListener;
// Objeto usado para commit manual do offset.
import org.springframework.kafka.support.Acknowledgment;
// Marca a classe como bean Spring.
import org.springframework.stereotype.Component;

// Consumer batch que processa eventos do topico principal.
@Component
public class SensorDataConsumer {

    // Logger da classe.
    private static final Logger log = LoggerFactory.getLogger(SensorDataConsumer.class);

    // Servico que valida cada registro do lote.
    private final SensorEventValidator sensorEventValidator;
    // Servico que persiste os registros aceitos.
    private final SensorEventPersistenceService persistenceService;
    // Servico que publica registros rejeitados em topicos de apoio.
    private final RejectedEventPublisher rejectedEventPublisher;
    // Servico que pausa o listener quando o banco entra sob pressao.
    private final ListenerBackpressureService listenerBackpressureService;
    // Servico que consolida metricas periodicas.
    private final IngestionMetricsService ingestionMetricsService;
    // Propriedades customizadas usadas por este consumer.
    private final IngestionProperties properties;

    // Construtor com injecao das dependencias necessarias ao consumo.
    public SensorDataConsumer(
            SensorEventValidator sensorEventValidator,
            SensorEventPersistenceService persistenceService,
            RejectedEventPublisher rejectedEventPublisher,
            ListenerBackpressureService listenerBackpressureService,
            IngestionMetricsService ingestionMetricsService,
            IngestionProperties properties) {
        this.sensorEventValidator = sensorEventValidator;
        this.persistenceService = persistenceService;
        this.rejectedEventPublisher = rejectedEventPublisher;
        this.listenerBackpressureService = listenerBackpressureService;
        this.ingestionMetricsService = ingestionMetricsService;
        this.properties = properties;
    }

    // Metodo executado automaticamente quando o listener recebe um lote do Kafka.
    @KafkaListener(
            id = "${iot.consumer.listener-id}",
            topics = "${iot.topic.name}",
            containerFactory = "batchKafkaListenerContainerFactory")
    public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
        // Se o poll vier vazio, nao ha nada para processar.
        if (records.isEmpty()) {
            return;
        }

        // Antes de processar o lote, verifica se o banco ja esta sob pressao.
        if (listenerBackpressureService.isDatabaseUnderPressure()) {
            // Pausa o listener para deixar o backlog no Kafka, nao na aplicacao.
            listenerBackpressureService.pauseListener(
                    properties.getConsumer().getListenerId(),
                    "Hikari pool usage exceeded threshold before batch persistence");
            // Lanca uma excecao transitoria para acionar a politica de retry do listener.
            throw new TransientDataAccessResourceException("TimescaleDB is under write pressure");
        }

        // Marca o inicio do processamento do lote para medir latencia total.
        long startedAt = System.nanoTime();
        // Lista que acumula apenas os eventos considerados validos.
        List<SensorDataEvent> acceptedEvents = new ArrayList<>(records.size());
        // Lista com os registros rejeitados, publicada apenas apos persistencia bem-sucedida.
        List<RejectedRecord> rejectedRecords = new ArrayList<>();
        // Contador de eventos rejeitados por payload invalido.
        int invalidCount = 0;
        // Contador de eventos rejeitados por device nao cadastrado.
        int unknownDeviceCount = 0;

        // Percorre todos os registros recebidos neste lote.
        for (ConsumerRecord<String, String> record : records) {
            // Valida o registro atual.
            SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);
            // Direciona o registro conforme o status final de validacao.
            switch (outcome.status()) {
                // Eventos aceitos entram no lote de persistencia.
                case ACCEPTED -> acceptedEvents.add(outcome.event());
                // Eventos invalidos sao enviados ao topico de invalidos.
                case INVALID -> {
                    invalidCount++;
                    rejectedRecords.add(RejectedRecord.invalid(record, outcome.deviceId(), outcome.reason()));
                }
                // Eventos com device desconhecido vao para topico especifico.
                case UNKNOWN_DEVICE -> {
                    unknownDeviceCount++;
                    rejectedRecords.add(RejectedRecord.unknownDevice(record, outcome.deviceId(), outcome.reason()));
                }
            }
        }

        try {
            // So grava no banco se houver pelo menos um evento valido no lote.
            if (!acceptedEvents.isEmpty()) {
                persistenceService.persistBatch(acceptedEvents);
            }
        } catch (DataAccessException exception) {
            // Se a escrita falhar, pausa o listener para aliviar o banco.
            listenerBackpressureService.pauseListener(
                    properties.getConsumer().getListenerId(),
                    "TimescaleDB write exception: " + exception.getClass().getSimpleName());
            // Registra a falha com contexto do tamanho do lote.
            log.warn("Failed to persist batch of {} events", acceptedEvents.size(), exception);
            // Relanca a excecao para que o handler de erro trate o retry.
            throw exception;
        }

        // Publica rejeicoes so depois que os eventos aceitos foram persistidos.
        for (RejectedRecord rejectedRecord : rejectedRecords) {
            if (rejectedRecord.unknownDevice()) {
                rejectedEventPublisher.publishUnknownDevice(
                        rejectedRecord.record(),
                        rejectedRecord.deviceId(),
                        rejectedRecord.reason());
            } else {
                rejectedEventPublisher.publishInvalid(
                        rejectedRecord.record(),
                        rejectedRecord.deviceId(),
                        rejectedRecord.reason());
            }
        }

        // Confirma o offset apenas apos terminar a validacao e a persistencia.
        acknowledgment.acknowledge();
        // Envia os numeros consolidados do lote para a janela de metricas.
        ingestionMetricsService.recordBatch(
                records.size(),
                acceptedEvents.size(),
                invalidCount,
                unknownDeviceCount,
                Duration.ofNanos(System.nanoTime() - startedAt));
    }

    // Estrutura local que preserva o contexto de uma rejeicao ate a fase de publicacao.
    private record RejectedRecord(
            ConsumerRecord<String, String> record,
            String deviceId,
            String reason,
            boolean unknownDevice) {

        private static RejectedRecord invalid(
                ConsumerRecord<String, String> record,
                String deviceId,
                String reason) {
            return new RejectedRecord(record, deviceId, reason, false);
        }

        private static RejectedRecord unknownDevice(
                ConsumerRecord<String, String> record,
                String deviceId,
                String reason) {
            return new RejectedRecord(record, deviceId, reason, true);
        }
    }
}
