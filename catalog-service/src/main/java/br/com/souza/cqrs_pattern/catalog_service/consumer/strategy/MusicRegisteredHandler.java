package br.com.souza.cqrs_pattern.catalog_service.consumer.strategy;

import br.com.souza.cqrs_pattern.catalog_service.consumer.KafkaMusicListener;
import br.com.souza.cqrs_pattern.catalog_service.dto.MusicEvent;
import br.com.souza.cqrs_pattern.catalog_service.service.MusicEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class MusicRegisteredHandler implements MusicEventHandlerStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicRegisteredHandler.class);
    private final ObjectMapper objectMapper;
    private final MusicEventService musicEventService;

    @Override
    public String eventType() {
        return "MusicRegistered";
    }

    @Override
    public void handle(String payload) {
        try {
            MusicEvent event = objectMapper.readValue(payload, MusicEvent.class);
            musicEventService.indexByArtist(event);
            LOGGER.info("Music indexed by artist");
            musicEventService.indexByGenre(event);
            LOGGER.info("Music indexed by gender");
        } catch (Exception e) {
            throw new RuntimeException("Failed to process MusicRegistered event", e);
        }
    }
}
