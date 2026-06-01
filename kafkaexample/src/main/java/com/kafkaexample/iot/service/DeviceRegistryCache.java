package com.kafkaexample.iot.service;

// Repositorio que acessa o cadastro de devices no banco.
import com.kafkaexample.iot.repository.DeviceRegistryRepository;
// Hook executado logo apos a criacao do bean.
import jakarta.annotation.PostConstruct;
// Interface de conjunto.
import java.util.Set;
// Logger SLF4J.
import org.slf4j.Logger;
// Factory do logger.
import org.slf4j.LoggerFactory;
// Anotacao usada para executar refresh periodico.
import org.springframework.scheduling.annotation.Scheduled;
// Marca a classe como bean de servico.
import org.springframework.stereotype.Service;

// Cache local de devices ativos, usado para evitar consulta ao banco por mensagem.
@Service
public class DeviceRegistryCache {

    // Logger da classe.
    private static final Logger log = LoggerFactory.getLogger(DeviceRegistryCache.class);

    // Repositorio usado para consultar o estado do cadastro.
    private final DeviceRegistryRepository deviceRegistryRepository;
    // Snapshot atomico que guarda os ids ativos em memoria.
    private volatile Set<String> activeDevices = Set.of();

    // Construtor com injecao do repositorio.
    public DeviceRegistryCache(DeviceRegistryRepository deviceRegistryRepository) {
        this.deviceRegistryRepository = deviceRegistryRepository;
    }

    // Carrega o cache assim que o bean termina de subir.
    @PostConstruct
    void loadOnStartup() {
        // Reaproveita a mesma rotina de refresh usada pelo scheduler.
        refresh();
    }

    // Atualiza periodicamente o conjunto de devices ativos.
    @Scheduled(
            fixedDelayString = "${iot.validation.device-cache-refresh-ms}",
            initialDelayString = "0")
    public void refresh() {
        // Troca a referencia do cache de uma vez para evitar janela de conjunto vazio.
        Set<String> latest = Set.copyOf(deviceRegistryRepository.findAllActiveDeviceIds());
        activeDevices = latest;
        // Registra o total de devices carregados para observabilidade.
        log.info("Device cache refreshed with {} active devices", latest.size());
    }

    // Informa se um determinado device esta ativo no cadastro.
    public boolean isRegistered(String deviceId) {
        // Faz uma consulta O(1) no conjunto em memoria.
        return activeDevices.contains(deviceId);
    }
}
