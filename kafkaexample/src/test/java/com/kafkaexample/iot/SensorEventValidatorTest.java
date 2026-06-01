package com.kafkaexample.iot;

// Assert usado para validar resultados dos testes.
import static org.assertj.core.api.Assertions.assertThat;
// Helper usado para configurar stubs do mock.
import static org.mockito.Mockito.when;

// Mapper Jackson usado para montar o validador de teste.
import com.fasterxml.jackson.databind.ObjectMapper;
// Modulo que ensina o Jackson a serializar e desserializar java.time.
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// Propriedades padrao da aplicacao para usar no teste.
import com.kafkaexample.iot.config.IngestionProperties;
// Cache de devices, mockado no teste.
import com.kafkaexample.iot.service.DeviceRegistryCache;
// Classe validada por este teste.
import com.kafkaexample.iot.service.SensorEventValidator;
// Registro Kafka usado como fixture de teste.
import org.apache.kafka.clients.consumer.ConsumerRecord;
// Hook executado antes de cada teste.
import org.junit.jupiter.api.BeforeEach;
// Marca metodos como casos de teste.
import org.junit.jupiter.api.Test;
// Biblioteca de mocks.
import org.mockito.Mockito;

// Testes unitarios basicos do validador de eventos.
class SensorEventValidatorTest {

    // Mock do cache de devices.
    private DeviceRegistryCache deviceRegistryCache;
    // Instancia do validador sob teste.
    private SensorEventValidator sensorEventValidator;

    // Prepara os objetos antes de cada caso de teste.
    @BeforeEach
    void setUp() {
        // Cria um mock do cache.
        deviceRegistryCache = Mockito.mock(DeviceRegistryCache.class);
        // Instancia o ObjectMapper com suporte a Instant.
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        // Monta o validador com dependencias reais e mockadas.
        sensorEventValidator = new SensorEventValidator(objectMapper, deviceRegistryCache, new IngestionProperties());
    }

    // Verifica que um payload valido e aceito quando o device esta cadastrado.
    @Test
    void shouldAcceptRegisteredDeviceWithValidPayload() {
        // Configura o mock para indicar que o device existe no cache.
        when(deviceRegistryCache.isRegistered("device-0001")).thenReturn(true);

        // Monta um registro Kafka valido.
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "sensor_data",
                0,
                1L,
                "device-0001",
                """
                {"deviceId":"device-0001","value":21.45,"timestamp":"2026-03-27T15:00:00Z"}
                """);

        // Executa a validacao.
        SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);

        // Confirma que o status final foi ACCEPTED.
        assertThat(outcome.status()).isEqualTo(SensorEventValidator.Status.ACCEPTED);
        // Confirma que o evento normalizado esta presente.
        assertThat(outcome.event()).isNotNull();
        // Confirma que o deviceId final e o esperado.
        assertThat(outcome.event().deviceId()).isEqualTo("device-0001");
        // Confirma que o metadado Kafka foi preservado para idempotencia.
        assertThat(outcome.event().sourceTopic()).isEqualTo("sensor_data");
        assertThat(outcome.event().sourcePartition()).isEqualTo(0);
        assertThat(outcome.event().sourceOffset()).isEqualTo(1L);
    }

    // Verifica que um payload de device nao cadastrado e desviado.
    @Test
    void shouldRejectUnknownDevice() {
        // Configura o mock para indicar que o device nao existe.
        when(deviceRegistryCache.isRegistered("device-9999")).thenReturn(false);

        // Monta um registro Kafka com device desconhecido.
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "sensor_data",
                0,
                1L,
                "device-9999",
                """
                {"deviceId":"device-9999","value":21.45,"timestamp":"2026-03-27T15:00:00Z"}
                """);

        // Executa a validacao.
        SensorEventValidator.ValidationOutcome outcome = sensorEventValidator.validate(record);

        // Confirma que o status final foi UNKNOWN_DEVICE.
        assertThat(outcome.status()).isEqualTo(SensorEventValidator.Status.UNKNOWN_DEVICE);
        // Confirma que o motivo tecnico da rejeicao foi preenchido corretamente.
        assertThat(outcome.reason()).isEqualTo("device_not_registered");
    }
}
