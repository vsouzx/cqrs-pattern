package br.com.souza.cqrs_pattern.catalog_service.service;

import br.com.souza.cqrs_pattern.catalog_service.database.document.ArtistCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.database.document.GenreCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.database.repository.ArtistCatalogRepository;
import br.com.souza.cqrs_pattern.catalog_service.database.repository.GenreCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ArtistCatalogRepository artistCatalogRepository;
    private final GenreCatalogRepository genreCatalogRepository;

    public List<ArtistCatalogDocument> getAllByArtist() {
        return artistCatalogRepository.findAll();
    }

    public Optional<ArtistCatalogDocument> getByArtist(String artistName) {
        return artistCatalogRepository.findById(artistName);
    }

    public List<GenreCatalogDocument> getAllByGenre() {
        return genreCatalogRepository.findAll();
    }

    public Optional<GenreCatalogDocument> getByGenre(String genre) {
        return genreCatalogRepository.findById(genre);
    }

}
