package br.com.souza.cqrs_pattern.catalog_service.database.repository;

import br.com.souza.cqrs_pattern.catalog_service.database.document.GenreCatalogDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GenreCatalogRepository extends MongoRepository<GenreCatalogDocument, String> {
}
