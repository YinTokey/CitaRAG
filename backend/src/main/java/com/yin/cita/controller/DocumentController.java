package com.yin.cita.controller;

import com.yin.cita.model.Document;
import com.yin.cita.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Sync: Save file & create DB record
            Document document = documentService.initiateUpload(file);

            // 2. Async: Trigger heavy processing
            documentService.processDocumentAsync(document.getId());

            // 3. Return immediately
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        System.out.println("REST Request to get all documents");
        List<Document> docs = documentService.getAllDocuments();
        System.out.println("Found " + docs.size() + " documents");
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> getDocumentContent(@PathVariable Long id) {
        return documentService.getDocumentById(id)
                .map(doc -> {
                    try {
                        Resource resource = documentService.loadAsResource(doc.getFilename());
                        String contentType = "application/octet-stream";
                        if (doc.getFilename().toLowerCase().endsWith(".pdf")) {
                            contentType = "application/pdf";
                        } else if (doc.getFilename().toLowerCase().endsWith(".txt")) {
                            contentType = "text/plain";
                        }

                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "inline; filename=\"" + doc.getFilename() + "\"")
                                .body(resource);
                    } catch (Exception e) {
                        return new ResponseEntity<Resource>(HttpStatus.NOT_FOUND);
                    }
                })
                .orElse(new ResponseEntity<Resource>(HttpStatus.NOT_FOUND));
    }
}
