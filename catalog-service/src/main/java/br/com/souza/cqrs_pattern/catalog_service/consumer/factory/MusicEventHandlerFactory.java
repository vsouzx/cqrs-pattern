package br.com.souza.cqrs_pattern.catalog_service.consumer.factory;

import br.com.souza.cqrs_pattern.catalog_service.consumer.strategy.MusicEventHandlerStrategy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MusicEventHandlerFactory {

    private final Map<String, MusicEventHandlerStrategy> strategies = new HashMap<>();

    public MusicEventHandlerFactory(Set<MusicEventHandlerStrategy> strategiesSet) {
        strategiesSet.forEach(strategy -> strategies.put(strategy.eventType(), strategy));
    }

    public MusicEventHandlerStrategy getStrategy(String eventType) {
        MusicEventHandlerStrategy handler = strategies.get(eventType);
        if (handler == null) {
            throw new IllegalArgumentException("Event type not supported");
        }
        return handler;
    }
}
