#!/bin/bash
KAFKA=kafka:9092

docker compose exec kafka kafka-topics --bootstrap-server $KAFKA --create --if-not-exists \
  --topic outbox.events.Musics \
  --partitions 3 \
  --config retention.ms=604800000

echo "Tópicos criados:"
docker compose exec kafka kafka-topics --bootstrap-server $KAFKA --list