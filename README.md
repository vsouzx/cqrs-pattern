# CQRS Pattern

Implementação de referência dos padrões **CQRS**, **Transactional Outbox** e **CDC** com dois microsserviços em Spring Boot 4.0 + Java 25.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Conceitos](#conceitos)
- [Diagrama de Fluxo](#diagrama-de-fluxo)
- [Infraestrutura](#infraestrutura)
- [music-service](#music-service-write-side)
- [catalog-service](#catalog-service-read-side)
- [Como Executar](#como-executar)
- [Exemplos de Requisição](#exemplos-de-requisição)

---
## Arquitetura

<img width="977" height="590" alt="Image" src="https://github.com/user-attachments/assets/200cabf2-011a-4264-8b40-b13b4af15180" />

---

## Visão Geral

O sistema gerencia um catálogo de músicas dividido em dois lados:

| Lado | Serviço | Responsabilidade |
|------|---------|-----------------|
| **Write** | `music-service` | Registra e remove músicas no MySQL |
| **Read** | `catalog-service` | Consulta o catálogo desnormalizado no MongoDB |

Nenhum serviço se comunica diretamente com o outro. A sincronização acontece de forma assíncrona via **Kafka**, alimentado pelo **Debezium** que monitora o binlog do MySQL.

---

## Conceitos

### CQRS — Command Query Responsibility Segregation

Separa as operações de **escrita** (commands) das de **leitura** (queries) em modelos e serviços distintos. Isso permite escalar e otimizar cada lado de forma independente — o write side é normalizado e consistente, o read side é desnormalizado e otimizado para consulta.

```
Commands  →  music-service  →  MySQL      (modelo normalizado)
Queries   →  catalog-service →  MongoDB   (modelo desnormalizado, pré-agregado)
```

### Transactional Outbox Pattern

Garante que o evento Kafka seja enviado **se e somente se** a transação de escrita for confirmada, eliminando o problema de dual-write (escrever no banco e publicar no Kafka são operações separadas que podem falhar independentemente).

**Como funciona:**
1. A escrita na tabela de negócio (`musics`) e a inserção na tabela de outbox (`outbox_events`) acontecem dentro da **mesma transação** `@Transactional`
2. Se a transação fizer rollback, o evento de outbox também é descartado — jamais há um evento sem o dado correspondente
3. O Debezium lê o outbox via CDC e publica no Kafka de forma confiável

### CDC — Change Data Capture

O Debezium monitora o **binlog do MySQL** (modo `ROW`) e captura cada INSERT na tabela `outbox_events` em tempo real, sem polling e sem impacto na aplicação.

O transform `EventRouter` lê o campo `aggregate_type` para rotear o evento para o tópico Kafka correto (`outbox.events.{aggregate_type}`), e promove `event_type` como header da mensagem.

### Idempotência do Consumidor

O `catalog-service` implementa idempotência em múltiplas camadas:

1. **Deduplicação por eventId**: antes de processar, verifica na coleção `processed_events` (MongoDB) se o evento já foi processado. O `_id` é o próprio `eventId`, garantindo unicidade atômica via `DuplicateKeyException`.

2. **Proteção contra eventos fora de ordem**: se já existe um evento mais recente (por timestamp) para o mesmo `aggregateId`, o evento atrasado é descartado. Isso evita inconsistências como um `MusicRegistered` sobrescrevendo um `MusicRemoved` que já foi processado.

3. **$addToSet como camada extra**: o MongoDB ignora duplicatas no subdocumento via `$addToSet`, servindo como última linha de defesa.

**Fluxo:**
```
Evento chega no Kafka
  → Verifica se eventId já existe em processed_events (skip se duplicata)
  → Verifica se já existe evento MAIS RECENTE para o aggregateId (skip se fora de ordem)
  → Insere atomicamente em processed_events
  → Processa o evento (indexa/remove no MongoDB)
  → Acknowledge
```

### Distributed Tracing & Observabilidade (ELK)

O rastreamento distribuído é implementado via **Micrometer Tracing + Brave**, com logs estruturados em JSON enviados ao **ELK Stack**.

**Fluxo de correlação end-to-end:**
1. O Brave gera `traceId`/`spanId` automaticamente para cada request HTTP no music-service e popula o SLF4J MDC
2. O `traceId` é persistido na coluna `trace_id` da tabela `outbox_events`
3. O Debezium propaga `trace_id` como header Kafka (`traceId`)
4. O catalog-service extrai o header e coloca no MDC como `traceparent`
5. O Logstash Logback Encoder envia logs JSON estruturados para o Logstash via TCP
6. O Elasticsearch indexa; o Kibana permite buscar por `traceId` ou `traceparent`

**Exemplo de log JSON enviado ao ELK:**
```json
{
  "@timestamp": "2026-05-03T15:58:51.842",
  "service": "music-service",
  "traceId": "64af3b2c1d2e3f4a",
  "spanId": "a1b2c3d4e5f6a7b8",
  "message": "Music registered successfully with ID: 9ab80add-...",
  "level": "INFO"
}
```

---

## Infraestrutura

Toda a infra sobe via Docker Compose.

| Componente | Porta | Descrição |
|-----------|-------|-----------|
| MySQL | 3307 | Banco de dados do write side. `binlog_format=ROW` habilitado para CDC |
| Zookeeper | 2181 | Coordenação do cluster Kafka |
| Kafka | 9092 / 29092 | Broker de mensagens. Auto-create de tópicos desabilitado |
| Kafka Connect | 8083 | Roda o conector Debezium |
| MongoDB | 27017 | Banco de dados do read side |
| Kafka UI | 8080 | Dashboard para monitorar tópicos e mensagens |
| Elasticsearch | 9200 | Armazenamento de logs estruturados |
| Logstash | 5044 | Ingestão de logs via TCP (JSON) |
| Kibana | 5601 | Visualização e busca de logs por traceId |
| Prometheus | 9090 | Coleta de métricas |
| Grafana | 3000 | Dashboards de métricas (Golden Signals) |


### Conector Debezium

O conector monitora exclusivamente a tabela `outbox_events` e aplica o transform `EventRouter`:

- **Roteamento:** `aggregate_type` → `outbox.events.{aggregate_type}`
- **Header:** `event_type` promovido como header `eventType`
- **Payload:** expandido de string JSON para objeto JSON (`expand.json.payload=true`)
- **Key:** `aggregate_id` (o `id` da música) vira a key da mensagem Kafka

### Limpeza automática

Um MySQL Event Scheduler remove registros da `outbox_events` com mais de 7 dias diariamente — o Debezium já terá capturado via binlog bem antes disso.

### Documentos MongoDB

O serviço mantém dois documentos por música — cada um otimizado para um padrão de consulta diferente.

**`catalog_by_artist`** — agrupa músicas por artista:
```json
{
  "_id": "veigh",
  "musics": [
    { "_id": "uuid-1", "musicName": "mil maneiras", "genre": "trap" },
    { "_id": "uuid-2", "musicName": "outro hit",    "genre": "trap" }
  ]
}
```

**`catalog_by_genre`** — agrupa músicas por gênero:
```json
{
  "_id": "trap",
  "musics": [
    { "_id": "uuid-1", "musicName": "mil maneiras",  "artistName": "veigh" },
    { "_id": "uuid-2", "musicName": "talvez você...", "artistName": "veigh" }
  ]
}
```

### Processamento de eventos

O `KafkaMusicListener` consome o tópico `outbox.events.Musics` com:
- **Ack mode:** `MANUAL_IMMEDIATE` — só confirma após processamento bem-sucedido
- **Retry:** exponential backoff (1s → 2s → 4s, máximo 3 tentativas)
- **Concorrência:** 3 threads consumidoras

Os handlers são registrados automaticamente na `MusicEventHandlerFactory` via `Set<MusicEventHandlerStrategy>`:

| Handler | Evento | Operação MongoDB |
|---------|--------|-----------------|
| `MusicRegisteredHandler` | `MusicRegistered` | `upsert` + `$addToSet` em ambos os documentos |
| `MusicRemovedHandler` | `MusicRemoved` | `updateFirst` + `$pull` em ambos os documentos |

**Idempotência:** o `$addToSet` garante que reentregas do Kafka não criem entradas duplicadas, pois o MongoDB compara o subdocumento completo antes de inserir.

---

## Como Executar

### 1. Subir a infraestrutura

```bash
docker-compose up -d
```

Aguarde todos os containers ficarem saudáveis (especialmente o Kafka Connect).


### 2. Registrar o conector Debezium

```bash
chmod +x create-connector.sh && ./create-connector.sh
```

### 3. Subir os serviços

```bash
# music-service
cd music-service && mvn spring-boot:run

# catalog-service (outro terminal)
cd catalog-service && mvn spring-boot:run
```

### Monitoramento

- **Kafka UI**: [http://localhost:8080](http://localhost:8080) — tópicos e mensagens em tempo real
- **Kibana**: [http://localhost:5601](http://localhost:5601) — logs estruturados com correlação por traceId
- **Grafana**: [http://localhost:3000](http://localhost:3000) (admin/admin) — métricas e dashboards

### Visualizando logs no Kibana

1. Acesse [http://localhost:5601](http://localhost:5601)
2. **Management** → **Stack Management** → **Data Views** → **Create data view**
3. Index pattern: `app-logs-*` | Timestamp field: `@timestamp` → **Save**
4. **Discover** → selecione `app-logs-*`
5. Para correlacionar o fluxo completo de um request:
   ```
   traceId: "copie-do-log-do-music-service"
   ```
   ou no catalog-service:
   ```
   traceparent: "mesmo-traceId"
   ```

---

## Exemplos de Requisição

### Registrar música

```bash
curl -X POST http://localhost:8081/v1/musics \
  -H "Content-Type: application/json" \
  -d '{
    "musicName": "mil maneiras",
    "artistName": "veigh",
    "genre": "trap"
  }'
```

### Remover música

```bash
curl -X DELETE http://localhost:8081/v1/musics/{id}
```

### Consultar catálogo por artista

```bash
curl http://localhost:8082/v1/catalog/by-artist/veigh
```

### Consultar catálogo por gênero

```bash
curl http://localhost:8082/v1/catalog/by-genre/trap
```

### Listar todos os artistas

```bash
curl http://localhost:8082/v1/catalog/by-artist
```
