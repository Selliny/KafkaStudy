#!/bin/sh
set -eu

BOOTSTRAP_SERVER="kafka:29092"
KAFKA_TOPICS_BIN="/opt/bitnami/kafka/bin/kafka-topics.sh"

echo "Waiting for Kafka controller quorum..."
until "${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" --list >/dev/null 2>&1; do
  sleep 2
done

"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data_invalid \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

"${KAFKA_TOPICS_BIN}" --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create --if-not-exists --topic sensor_data_unknown_device \
  --partitions 12 --replication-factor 1 \
  --config min.insync.replicas=1

echo "Topics created successfully."
