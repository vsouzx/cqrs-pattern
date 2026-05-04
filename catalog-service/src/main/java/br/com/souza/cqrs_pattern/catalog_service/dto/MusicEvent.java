package br.com.souza.cqrs_pattern.catalog_service.dto;

import java.time.LocalDateTime;

public record MusicEvent(String id, String musicName, String artistName, String genre,
                         LocalDateTime createdAt) {
}
