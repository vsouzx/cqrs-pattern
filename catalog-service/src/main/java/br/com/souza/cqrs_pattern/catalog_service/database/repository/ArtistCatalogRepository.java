package br.com.souza.cqrs_pattern.catalog_service.database.repository;

import br.com.souza.cqrs_pattern.catalog_service.database.document.ArtistCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtistCatalogRepository extends MongoRepository<ArtistCatalogDocument, String> {
}
