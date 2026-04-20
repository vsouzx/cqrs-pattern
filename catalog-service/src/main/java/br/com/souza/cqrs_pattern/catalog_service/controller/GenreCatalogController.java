package br.com.souza.cqrs_pattern.catalog_service.controller;

import br.com.souza.cqrs_pattern.catalog_service.database.document.GenreCatalogDocument;
import br.com.souza.cqrs_pattern.catalog_service.database.repository.GenreCatalogRepository;
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
@RequestMapping("/v1/catalog/by-genre")
@RequiredArgsConstructor
public class GenreCatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<GenreCatalogDocument>> getAllByGenre() {
        return new ResponseEntity<>(catalogService.getAllByGenre(), HttpStatus.OK);
    }

    @GetMapping("/{genre}")
    public ResponseEntity<GenreCatalogDocument> getByGenre(@PathVariable String genre) {
        return catalogService.getByGenre(genre)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
