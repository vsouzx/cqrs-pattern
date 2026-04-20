package br.com.souza.cqrs_pattern.music_service.controller;

import br.com.souza.cqrs_pattern.music_service.dto.RegisterMusicRequestDTO;
import br.com.souza.cqrs_pattern.music_service.service.MusicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/musics")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    @PostMapping
    public void registerMusic(@RequestBody @Valid RegisterMusicRequestDTO request) throws Exception {
        musicService.registerMusic(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMusic(@PathVariable String id) throws Exception {
        musicService.removeMusic(id);
        return ResponseEntity.noContent().build();
    }
}
