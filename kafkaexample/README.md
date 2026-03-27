# Kafka IoT MVP

MVP de ingestão IoT focado em fundamentos de Apache Kafka com arquitetura híbrida:

- Infra local em containers: Kafka KRaft e TimescaleDB/Postgres 16.
- Aplicação Spring Boot 3.x executada no host.
- Simulador Python assíncrono executado no host.

## Pre-requisitos

- Docker Desktop com `docker compose`.
- JDK 21 no host. Confirme com `.\mvnw.cmd -version`; o campo `Java version` precisa mostrar `21`.
- Se `java -version` e `.\mvnw.cmd -version` mostrarem versões diferentes, ajuste o `JAVA_HOME`. O Maven Wrapper usa `JAVA_HOME`.
- O projeto continua fixado em Kafka `3.8.0`, mas o `docker-compose` usa `bitnamilegacy/kafka:3.8.0` porque as tags versionadas deixaram o catálogo público `bitnami/*`.
- O broker Kafka publicado para o host usa `localhost:39092`. A porta `9092` continua sendo usada apenas dentro do container.
- O TimescaleDB publicado para o host usa `localhost:55432` para evitar conflito com outras instâncias locais de Postgres.

## Estrutura

```text
.
|-- docker-compose.yml
|-- docker
|   |-- kafka
|   |   `-- create-topics.sh
|   `-- timescaledb
|       `-- init
|           `-- 001_init.sql
|-- simulator
|   |-- producer.py
|   `-- requirements.txt
|-- src
|   |-- main
|   |   |-- java/com/kafkaexample/iot
|   |   |   |-- KafkaExampleApplication.java
|   |   |   |-- config
|   |   |   |-- consumer
|   |   |   |-- dto
|   |   |   |-- entity
|   |   |   |-- repository
|   |   |   `-- service
|   |   `-- resources/application.yaml
|   `-- test/java/com/kafkaexample/iot/SensorEventValidatorTest.java
`-- pom.xml
```

## Decisões Técnicas

### Partições e replicação

- `sensor_data` foi criado com `12` partições. Em `1000 msg/s`, isso deixa uma média de `~83 msg/s` por partição, o que reduz contenção de escrita, preserva ordem por `deviceId` e cria margem para `concurrency=6` no consumer sem desperdiçar CPU em um host de `8 vCPUs`.
- No `docker-compose` local o fator de replicação é `1`, porque existe apenas um broker. Para um ambiente HA-ready, a configuração correta é `3 brokers`, `replication.factor=3` e `min.insync.replicas=2`. Isso permite sobreviver à perda de um broker sem aceitar escrita em líder isolado.

### Backpressure e risco de lag

- O consumer usa `batch listener`, `manual ack`, `max.poll.records=500` e `concurrency=6`.
- Se o pool Hikari ultrapassar `85%` de uso ou houver erro de escrita no TimescaleDB, o listener é pausado por `5s`, não confirma offsets e volta a consumir depois do cooldown.
- Esse comportamento faz o `lag` crescer de forma controlada, transferindo a pressão para o Kafka, que é o buffer correto, em vez de derrubar a aplicação ou perder mensagens já lidas.

### Contrato JSON

- O contrato aceito é `{"deviceId":"device-0001","value":21.45,"timestamp":"2026-03-27T15:00:00Z"}`.
- Payload vazio, JSON malformado, `deviceId` ausente, `value` ausente, `timestamp` ausente e chave Kafka divergente do `deviceId` são rejeitados.
- Dispositivos não cadastrados são desviados para `sensor_data_unknown_device`.
- Eventos inválidos vão para `sensor_data_invalid`.
- O cache de dispositivos ativos é carregado em memória e atualizado a cada `60s`, evitando consulta ao banco por mensagem.

## Inicialização

### 1. Subir a infraestrutura

```powershell
docker compose up -d
docker compose ps
```

Espere o `kafka-init` finalizar e confirme que o tópico existe:

```powershell
docker compose logs kafka-init
docker compose exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic sensor_data
```

### 2. Subir a aplicação Spring Boot

Valide o JDK antes de executar o Maven:

```powershell
.\mvnw.cmd -version
```

Se o comando acima não mostrar Java 21, ajuste o `JAVA_HOME` antes de continuar.

```powershell
.\mvnw.cmd spring-boot:run
```

O consumer group configurado é `iot-sensor-processor`.

### 3. Instalar dependências do simulador

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r .\simulator\requirements.txt
```

### 4. Gerar carga

Carga nominal de `1000 msg/s`:

```powershell
python .\simulator\producer.py --rate 1000 --devices 5000
```

Validação de rejeições:

```powershell
python .\simulator\producer.py --rate 1000 --devices 5000 --invalid-ratio 0.01 --unknown-device-ratio 0.01
```

## Como validar a taxa

### Logs do simulador

O simulador imprime janelas de `5s` com:

- `submitted_rate`: taxa de envio tentada.
- `delivered_rate`: taxa confirmada pelo broker.

Se o objetivo é `1000 msg/s`, o `delivered_rate` deve estabilizar próximo disso.

### Logs da aplicação

A aplicação imprime uma janela a cada `10s`:

- `persistedRate`: taxa efetiva gravada no TimescaleDB.
- `invalid`: quantidade rejeitada por contrato.
- `unknownDevices`: quantidade desviada por device não cadastrado.

Se `persistedRate` ficar muito abaixo da taxa entregue pelo simulador, o banco virou o gargalo e o lag deve aparecer no Kafka.

### Offset Explorer

Após subir o `docker-compose`, configure:

- `Cluster Name`: `IoT-Study-Cluster`
- Aba `Advanced`: selecione `Bootstrap Servers`
- `Bootstrap Servers`: `localhost:39092`

Validação:

- Em `Topics > sensor_data > Partitions`, verifique distribuição equilibrada entre as `12` partições.
- Em `Consumer Groups`, localize `iot-sensor-processor`.
- Se a coluna `Lag` subir continuamente durante `1000 msg/s`, o consumer não está conseguindo drenar o backlog na mesma velocidade.

## Verificação adicional

### Contagem no TimescaleDB

```powershell
docker compose exec timescaledb psql -U iot_user -d iot -c "SELECT count(*) FROM sensor_measurements;"
```

### Endpoints do Actuator

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

## Observações

- O Compose local publica Kafka e Postgres em `localhost`, mas mantém cada serviço em rede interna distinta, preservando a separação entre plano de streaming e plano de storage.
- O bootstrap do TimescaleDB já cria a hypertable `sensor_measurements`, os índices e a política de retenção de `30 dias` sem compressão.
