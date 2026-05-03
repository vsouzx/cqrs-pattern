package br.com.souza.cqrs_pattern.catalog_service.service;

import br.com.souza.cqrs_pattern.catalog_service.database.document.ProcessedEventDocument;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyService.class);
    private final MongoTemplate mongoTemplate;

    public boolean tryAcquire(String eventId, String aggregateId, String eventType, Instant eventTimestamp) {
        // 1. Verificar duplicata exata pelo eventId (unique _id)
        if (mongoTemplate.exists(new Query(Criteria.where("_id").is(eventId)), ProcessedEventDocument.class)) {
            LOGGER.warn("Event {} already processed — skipping (duplicate)", eventId);
            return false;
        }

        // 2. Verificar ordenação: se já processamos um evento MAIS RECENTE para este aggregate,
        //    o evento atual está fora de ordem e deve ser descartado.
        //    Ex: MusicRemoved (t2) já processado, MusicRegistered (t1) chega atrasado → skip
        var latestForAggregate = mongoTemplate.findOne(
                new Query(Criteria.where("aggregateId").is(aggregateId))
                        .with(Sort.by(Sort.Direction.DESC, "eventTimestamp"))
                        .limit(1),
                ProcessedEventDocument.class
        );

        if (latestForAggregate != null && latestForAggregate.getEventTimestamp().isAfter(eventTimestamp)) {
            LOGGER.warn("Event {} is out of order for aggregate {} — latest processed event is {} at {} — skipping",
                    eventId, aggregateId, latestForAggregate.getEventId(), latestForAggregate.getEventTimestamp());
            return false;
        }

        // 3. Registrar evento como processado (insert atômico — _id unique garante idempotência em race condition)
        try {
            mongoTemplate.insert(ProcessedEventDocument.builder()
                    .eventId(eventId)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventTimestamp(eventTimestamp)
                    .processedAt(Instant.now())
                    .build());
            return true;
        } catch (DuplicateKeyException e) {
            LOGGER.warn("Event {} already processed (concurrent insert) — skipping", eventId);
            return false;
        }
    }
}
