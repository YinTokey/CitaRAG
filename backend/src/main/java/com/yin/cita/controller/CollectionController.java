package com.yin.cita.controller;

import com.yin.cita.model.Collection;
import com.yin.cita.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final DocumentService documentService;

    public CollectionController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<Collection> createCollection(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String description = payload.get("description");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(documentService.createCollection(name, description));
    }

    @GetMapping
    public ResponseEntity<List<Collection>> getAllCollections() {
        return ResponseEntity.ok(documentService.getAllCollections());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Collection> getCollectionById(@PathVariable Long id) {
        return documentService.getCollectionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> addDocumentToCollection(@PathVariable Long id, @PathVariable Long docId) {
        try {
            documentService.addDocumentToCollection(id, docId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> removeDocumentFromCollection(@PathVariable Long id, @PathVariable Long docId) {
        try {
            documentService.removeDocumentFromCollection(id, docId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
