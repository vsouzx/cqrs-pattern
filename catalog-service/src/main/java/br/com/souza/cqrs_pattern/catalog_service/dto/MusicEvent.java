package br.com.souza.cqrs_pattern.catalog_service.dto;

public record MusicEvent(String id, String musicName, String artistName, String genre) {
}
