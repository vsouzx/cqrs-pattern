package br.com.souza.cqrs_pattern.music_service.service;

import br.com.souza.cqrs_pattern.music_service.database.model.MusicsEntity;
import br.com.souza.cqrs_pattern.music_service.database.model.OutboxEventEntity;
import br.com.souza.cqrs_pattern.music_service.database.repository.MusicsRepository;
import br.com.souza.cqrs_pattern.music_service.database.repository.OutboxEventRepository;
import br.com.souza.cqrs_pattern.music_service.dto.RegisterMusicRequestDTO;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MusicService {

    private static final Logger log = LoggerFactory.getLogger(MusicService.class);
    private final MusicsRepository musicsRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    @Transactional(rollbackFor = Exception.class)
    public void registerMusic(RegisterMusicRequestDTO request) throws Exception {
        try {
            if (musicsRepository.existsByMusicNameAndArtistName(request.musicName(), request.artistName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Music '%s' by '%s' already exists".formatted(request.musicName(), request.artistName()));
            }

            MusicsEntity music = musicsRepository.save(MusicsEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .artistName(request.artistName())
                    .musicName(request.musicName())
                    .genre(request.genre())
                    .createdAt(LocalDateTime.now())
                    .build());

            outboxEventRepository.save(OutboxEventEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("Musics")
                    .aggregateId(music.getId())
                    .eventType("MusicRegistered")
                    .payload(objectMapper.writeValueAsString(music))
                    .traceId(currentTraceId())
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("Music registered successfully with ID: {}", music.getId());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error to register music", e);
            throw new Exception("Error to register music", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeMusic(String id) throws Exception {
        try {
            MusicsEntity music = musicsRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Music not found"));

            musicsRepository.delete(music);

            outboxEventRepository.save(OutboxEventEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .aggregateType("Musics")
                    .aggregateId(music.getId())
                    .eventType("MusicRemoved")
                    .payload(objectMapper.writeValueAsString(music))
                    .traceId(currentTraceId())
                    .createdAt(LocalDateTime.now())
                    .build());
            log.info("Music removed successfully with ID: {}", id);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error removing music with ID: {}", id, e);
            throw new Exception("Error removing music", e);
        }
    }

    private String currentTraceId() {
        var span = tracer.currentSpan();
        return span != null ? span.context().traceId() : null;
    }
}
