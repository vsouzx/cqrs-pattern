#!/bin/bash

CONNECT_URL="http://localhost:8083/connectors"
JSON_FILE="./debezium-connector.json"

echo "Aguardando Kafka Connect ficar pronto..."
until curl -s --max-time 5 http://localhost:8083/ > /dev/null 2>&1; do
  echo "  Kafka Connect ainda não está pronto, tentando novamente em 5s..."
  sleep 5
done

echo "Kafka Connect pronto! Registrando conector..."
curl -X POST "$CONNECT_URL" \
  -H "Content-Type: application/json" \
  --data @"$JSON_FILE"