package com.kafkaexample.iot.config;

// Anotacao que mapeia um prefixo do YAML para esta classe.
import org.springframework.boot.context.properties.ConfigurationProperties;

// Indica que esta classe recebe dados do bloco "iot" do application.yaml.
@ConfigurationProperties(prefix = "iot")
public class IngestionProperties {

    // Bloco de propriedades relacionadas aos topicos Kafka.
    private final Topic topic = new Topic();
    // Bloco de propriedades relacionadas ao consumer.
    private final Consumer consumer = new Consumer();
    // Bloco com limites de validacao do payload.
    private final Validation validation = new Validation();
    // Bloco com parametros de backpressure.
    private final Backpressure backpressure = new Backpressure();
    // Bloco com parametros de observabilidade.
    private final Observability observability = new Observability();

    // Retorna a secao de topicos.
    public Topic getTopic() {
        return topic;
    }

    // Retorna a secao de consumer.
    public Consumer getConsumer() {
        return consumer;
    }

    // Retorna a secao de validacao.
    public Validation getValidation() {
        return validation;
    }

    // Retorna a secao de backpressure.
    public Backpressure getBackpressure() {
        return backpressure;
    }

    // Retorna a secao de observabilidade.
    public Observability getObservability() {
        return observability;
    }

    // Classe interna usada para agrupar nomes e parametros dos topicos.
    public static class Topic {
        // Nome do topico principal que recebe eventos validos.
        private String name = "sensor_data";
        // Nome do topico para eventos rejeitados por contrato invalido.
        private String invalidName = "sensor_data_invalid";
        // Nome do topico para eventos de devices nao cadastrados.
        private String unknownDeviceName = "sensor_data_unknown_device";
        // Nome do topico de dead-letter do pipeline principal.
        private String dltName = "sensor_data.DLT";
        // Numero de particoes do topico.
        private int partitions = 12;
        // Fator de replicacao padrao para o ambiente local.
        private short replicationFactor = 1;

        // Retorna o nome do topico principal.
        public String getName() {
            return name;
        }

        // Permite sobrescrever o nome do topico principal via YAML.
        public void setName(String name) {
            this.name = name;
        }

        // Retorna o nome do topico de invalidos.
        public String getInvalidName() {
            return invalidName;
        }

        // Permite sobrescrever o nome do topico de invalidos via YAML.
        public void setInvalidName(String invalidName) {
            this.invalidName = invalidName;
        }

        // Retorna o nome do topico de devices desconhecidos.
        public String getUnknownDeviceName() {
            return unknownDeviceName;
        }

        // Permite sobrescrever o nome do topico de devices desconhecidos via YAML.
        public void setUnknownDeviceName(String unknownDeviceName) {
            this.unknownDeviceName = unknownDeviceName;
        }

        // Retorna o nome do topico de dead-letter.
        public String getDltName() {
            return dltName;
        }

        // Permite sobrescrever o nome do topico de dead-letter via YAML.
        public void setDltName(String dltName) {
            this.dltName = dltName;
        }

        // Retorna a quantidade de particoes.
        public int getPartitions() {
            return partitions;
        }

        // Permite configurar a quantidade de particoes via YAML.
        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        // Retorna o fator de replicacao configurado.
        public short getReplicationFactor() {
            return replicationFactor;
        }

        // Permite configurar o fator de replicacao via YAML.
        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }
    }

    // Classe interna usada para propriedades de consumo Kafka.
    public static class Consumer {
        // Identificador do listener registrado no Spring Kafka.
        private String listenerId = "sensor-data-listener";
        // Numero de threads de consumo concorrente.
        private int concurrency = 6;
        // Numero maximo de mensagens retornadas por poll.
        private int maxPollRecords = 500;
        // Quantidade minima de bytes para o broker responder ao fetch.
        private int fetchMinBytes = 65536;
        // Tempo maximo que o broker espera para preencher o fetch.
        private int fetchMaxWaitMs = 50;
        // Limite de bytes por particao em cada fetch.
        private int maxPartitionFetchBytes = 1048576;

        // Retorna o id do listener.
        public String getListenerId() {
            return listenerId;
        }

        // Permite configurar o id do listener via YAML.
        public void setListenerId(String listenerId) {
            this.listenerId = listenerId;
        }

        // Retorna a concorrencia do listener.
        public int getConcurrency() {
            return concurrency;
        }

        // Permite configurar a concorrencia via YAML.
        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }

        // Retorna o maximo de registros por poll.
        public int getMaxPollRecords() {
            return maxPollRecords;
        }

        // Permite configurar o limite de registros por poll.
        public void setMaxPollRecords(int maxPollRecords) {
            this.maxPollRecords = maxPollRecords;
        }

        // Retorna o minimo de bytes por fetch.
        public int getFetchMinBytes() {
            return fetchMinBytes;
        }

        // Permite configurar o minimo de bytes por fetch.
        public void setFetchMinBytes(int fetchMinBytes) {
            this.fetchMinBytes = fetchMinBytes;
        }

        // Retorna a espera maxima do fetch.
        public int getFetchMaxWaitMs() {
            return fetchMaxWaitMs;
        }

        // Permite configurar a espera maxima do fetch.
        public void setFetchMaxWaitMs(int fetchMaxWaitMs) {
            this.fetchMaxWaitMs = fetchMaxWaitMs;
        }

        // Retorna o limite de bytes buscados por particao.
        public int getMaxPartitionFetchBytes() {
            return maxPartitionFetchBytes;
        }

        // Permite configurar o limite de bytes por particao.
        public void setMaxPartitionFetchBytes(int maxPartitionFetchBytes) {
            this.maxPartitionFetchBytes = maxPartitionFetchBytes;
        }
    }

    // Classe interna usada para limites de validacao.
    public static class Validation {
        // Tamanho maximo aceito para o deviceId.
        private int maxDeviceIdLength = 64;
        // Frequencia de atualizacao do cache de devices ativos.
        private long deviceCacheRefreshMs = 60000;

        // Retorna o limite do deviceId.
        public int getMaxDeviceIdLength() {
            return maxDeviceIdLength;
        }

        // Permite configurar o limite do deviceId.
        public void setMaxDeviceIdLength(int maxDeviceIdLength) {
            this.maxDeviceIdLength = maxDeviceIdLength;
        }

        // Retorna o intervalo de refresh do cache.
        public long getDeviceCacheRefreshMs() {
            return deviceCacheRefreshMs;
        }

        // Permite configurar o refresh do cache.
        public void setDeviceCacheRefreshMs(long deviceCacheRefreshMs) {
            this.deviceCacheRefreshMs = deviceCacheRefreshMs;
        }
    }

    // Classe interna usada para parametros de protecao contra sobrecarga.
    public static class Backpressure {
        // Percentual de uso do Hikari que dispara pausa do listener.
        private double hikariUsageThreshold = 0.85d;
        // Tempo de pausa do listener antes da retomada.
        private long pauseMs = 5000L;

        // Retorna o threshold de uso do Hikari.
        public double getHikariUsageThreshold() {
            return hikariUsageThreshold;
        }

        // Permite configurar o threshold de uso do Hikari.
        public void setHikariUsageThreshold(double hikariUsageThreshold) {
            this.hikariUsageThreshold = hikariUsageThreshold;
        }

        // Retorna o tempo de pausa do listener.
        public long getPauseMs() {
            return pauseMs;
        }

        // Permite configurar o tempo de pausa do listener.
        public void setPauseMs(long pauseMs) {
            this.pauseMs = pauseMs;
        }
    }

    // Classe interna usada para configuracoes de logs e metricas.
    public static class Observability {
        // Janela periodica usada para imprimir metricas consolidadas.
        private long logIntervalMs = 10000L;

        // Retorna o intervalo de log.
        public long getLogIntervalMs() {
            return logIntervalMs;
        }

        // Permite configurar o intervalo de log.
        public void setLogIntervalMs(long logIntervalMs) {
            this.logIntervalMs = logIntervalMs;
        }
    }
}
