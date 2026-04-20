package br.com.souza.cqrs_pattern.catalog_service.database.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Data
@Document(collection = "catalog_by_artist")
public class ArtistCatalogDocument {

    @Id
    private String artistName;
    private Set<MusicEntry> musics = new HashSet<>();

    public record MusicEntry(String id, String musicName, String genre) {}
}
