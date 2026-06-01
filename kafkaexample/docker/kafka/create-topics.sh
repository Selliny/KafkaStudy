#!/bin/sh

# Encerra o script no primeiro erro e falha em uso de variavel indefinida.
set -eu

# Endereco interno do broker Kafka dentro da rede Docker.
BOOTSTRAP_SERVER="kafka:29092"
# Caminho da ferramenta de administracao de topicos dentro da imagem.
KAFKA_TOPICS_BIN="/opt/bitnami/kafka/bin/kafka-topics.sh"

# Log inicial para facilitar observabilidade do bootstrap.
echo "Waiting for Kafka controller quorum..."
# Aguarda o broker ficar apto a responder antes de criar os topicos.
until "${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" --list >/dev/null 2>&1; do
  # Espera curta entre tentativas.
  sleep 2
done

# Cria o topico principal que recebera eventos validos.
"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

# Cria o topico usado para payloads invalidos.
"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data_invalid \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

# Cria o topico usado para devices nao cadastrados.
"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data_unknown_device \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

# Cria o topico de dead-letter para falhas permanentes do consumer principal.
"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data.DLT \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

# Log final do bootstrap.
echo "Topics created successfully."
