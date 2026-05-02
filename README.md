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

O `catalog-service` usa `$addToSet` do MongoDB ao indexar músicas: se o mesmo evento chegar mais de uma vez (redelivery do Kafka), o banco ignora a duplicata silenciosamente. A remoção usa `$pull` com filtro pelo `id` único da música.

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

Acesse o **Kafka UI** em [http://localhost:8080](http://localhost:8080) para visualizar os tópicos e mensagens em tempo real.

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
