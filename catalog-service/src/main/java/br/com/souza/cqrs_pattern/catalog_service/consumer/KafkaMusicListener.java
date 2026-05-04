package br.com.souza.cqrs_pattern.catalog_service.consumer;

import br.com.souza.cqrs_pattern.catalog_service.consumer.factory.MusicEventHandlerFactory;
import br.com.souza.cqrs_pattern.catalog_service.dto.MusicEvent;
import br.com.souza.cqrs_pattern.catalog_service.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class KafkaMusicListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMusicListener.class);
    private final MusicEventHandlerFactory musicEventHandlerFactory;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "outbox.events.Musics")
    public void dispatchEvent(ConsumerRecord<String, String> consumerRecord,
                              @Header("eventType") String eventType,
                              @Header("id") String eventId,
                              @Header(name = "traceId", required = false) String originTraceId,
                              Acknowledgment acknowledgment) {
        try {
            if (originTraceId != null && !originTraceId.isBlank()) {
                MDC.put("traceparent", originTraceId);
            }
            LOGGER.info("Processing event {}: {}", eventId, consumerRecord.value());

            MusicEvent musicEvent = objectMapper.readValue(consumerRecord.value(), MusicEvent.class);
            Instant eventTimestamp = musicEvent.createdAt().toInstant(ZoneOffset.UTC);
            String aggregateId = consumerRecord.key();

            if (!idempotencyService.tryAcquire(eventId, aggregateId, eventType, eventTimestamp)) {
                acknowledgment.acknowledge();
                return;
            }

            musicEventHandlerFactory.getStrategy(eventType).handle(consumerRecord.value());
            acknowledgment.acknowledge();
            LOGGER.info("Event {} successfully processed", eventId);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid event type: {}", eventType, e);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            LOGGER.error("Error processing event {}: {}", eventId, e.getMessage(), e);
        } finally {
            MDC.remove("traceparent");
        }
    }
}
