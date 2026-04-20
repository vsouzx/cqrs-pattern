package br.com.souza.cqrs_pattern.music_service.database.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "musics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MusicsEntity {

    @Id
    private String id;

    @Column(name = "music_name", nullable = false)
    private String musicName;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    @Column(name = "genre", nullable = false)
    private String genre;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}