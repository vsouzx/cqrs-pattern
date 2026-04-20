package br.com.souza.cqrs_pattern.catalog_service.consumer;

import br.com.souza.cqrs_pattern.catalog_service.consumer.factory.MusicEventHandlerFactory;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaMusicListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMusicListener.class);
    private final MusicEventHandlerFactory musicEventHandlerFactory;

    @KafkaListener(topics = "outbox.events.Musics")
    public void dispatchEvent(ConsumerRecord<String, String> consumerRecord,
                              @Header("eventType") String eventType,
                              @Header("id") String eventId,
                              Acknowledgment acknowledgment) {
        try {
            LOGGER.info("Initing event {} processing: {}", eventId, consumerRecord.value());
            musicEventHandlerFactory.getStrategy(eventType).handle(consumerRecord.value());
            acknowledgment.acknowledge();
            LOGGER.info("Event {} successfully processed", eventId);
        }catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid event type: {}", eventType, e);
            acknowledgment.acknowledge();
        }catch (Exception e) {
            LOGGER.error("Error processing event {}: {}", eventId, e.getMessage(), e);
        }
    }
}
