package br.com.souza.cqrs_pattern.catalog_service.consumer.strategy;

import br.com.souza.cqrs_pattern.catalog_service.dto.MusicEvent;
import br.com.souza.cqrs_pattern.catalog_service.service.MusicEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class MusicRemovedHandler implements MusicEventHandlerStrategy {

    private final ObjectMapper objectMapper;
    private final MusicEventService musicEventService;

    @Override
    public String eventType() {
        return "MusicRemoved";
    }

    @Override
    public void handle(String payload) {
        try {
            MusicEvent event = objectMapper.readValue(payload, MusicEvent.class);
            musicEventService.removeByArtist(event);
            musicEventService.removeByGenre(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process MusicRemoved event", e);
        }
    }
}
