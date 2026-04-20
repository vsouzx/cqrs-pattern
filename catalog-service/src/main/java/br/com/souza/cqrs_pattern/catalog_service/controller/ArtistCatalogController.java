package br.com.souza.cqrs_pattern.catalog_service.controller;

import br.com.souza.cqrs_pattern.catalog_service.database.document.ArtistCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.database.repository.ArtistCatalogRepository;
import br.com.souza.cqrs_pattern.catalog_service.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/catalog/by-artist")
@RequiredArgsConstructor
public class ArtistCatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<ArtistCatalogDocument>> getAllByArtist() {
        return new ResponseEntity<>(catalogService.getAllByArtist(), HttpStatus.OK);
    }

    @GetMapping("/{artistName}")
    public ResponseEntity<ArtistCatalogDocument> getByArtist(@PathVariable String artistName) {
        return catalogService.getByArtist(artistName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
