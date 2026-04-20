package br.com.souza.cqrs_pattern.music_service.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterMusicRequestDTO(@NotBlank String musicName,
                                      @NotBlank String artistName,
                                      @NotBlank String genre) {
}
