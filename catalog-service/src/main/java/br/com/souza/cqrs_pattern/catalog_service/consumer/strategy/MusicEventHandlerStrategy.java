package br.com.souza.cqrs_pattern.catalog_service.consumer.strategy;

public interface MusicEventHandlerStrategy {

    String eventType();

    void handle(String payload);
}
