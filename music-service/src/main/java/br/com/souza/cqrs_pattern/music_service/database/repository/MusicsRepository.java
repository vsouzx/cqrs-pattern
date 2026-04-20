package br.com.souza.cqrs_pattern.music_service.database.repository;

import br.com.souza.cqrs_pattern.music_service.database.model.MusicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MusicsRepository extends JpaRepository<MusicsEntity, String> {

    boolean existsByMusicNameAndArtistName(String musicName, String artistName);
}