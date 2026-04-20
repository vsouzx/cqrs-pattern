package br.com.souza.cqrs_pattern.catalog_service.service;

import br.com.souza.cqrs_pattern.catalog_service.database.document.ArtistCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.database.document.GenreCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.dto.MusicEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MusicEventService {

    private final MongoTemplate mongoTemplate;

    public void indexByArtist(MusicEvent event) {
        var entry = new ArtistCatalogDocument.MusicEntry(event.id(), event.musicName(), event.genre());
        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(event.artistName())),
                new Update().addToSet("musics", entry),
                ArtistCatalogDocument.class
        );
    }

    public void indexByGenre(MusicEvent event) {
        var entry = new GenreCatalogDocument.MusicEntry(event.id(), event.musicName(), event.artistName());
        mongoTemplate.upsert(
                new Query(Criteria.where("_id").is(event.genre())),
                new Update().addToSet("musics", entry),
                GenreCatalogDocument.class
        );
    }

    public void removeByArtist(MusicEvent event) {
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(event.artistName())),
                new Update().pull("musics", Query.query(Criteria.where("_id").is(event.id()))),
                ArtistCatalogDocument.class
        );
    }

    public void removeByGenre(MusicEvent event) {
        mongoTemplate.updateFirst(
                new Query(Criteria.where("_id").is(event.genre())),
                new Update().pull("musics", Query.query(Criteria.where("_id").is(event.id()))),
                GenreCatalogDocument.class
        );
    }
}
