package com.yin.cita.controller;

import com.yin.cita.model.Document;
import com.yin.cita.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final DocumentService documentService;

    @Autowired
    public FileUploadController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body("No files uploaded");
        }

        StringBuilder resultLog = new StringBuilder("Processed files:\n");
        int successCount = 0;

        for (MultipartFile file : files) {
            try {
                // Delegate to DocumentService which handles:
                // 1. Parsing, 2. Chunking, 3. Vector Storage, 4. DB Persistence
                Document savedDoc = documentService.uploadAndParse(file);

                resultLog.append("- ").append(file.getOriginalFilename())
                        .append(": Success (ID: ").append(savedDoc.getId()).append(")\n");
                successCount++;

            } catch (Exception e) {
                e.printStackTrace();
                resultLog.append("- ").append(file.getOriginalFilename()).append(": FAILED - ").append(e.getMessage())
                        .append("\n");
            }
        }

        return ResponseEntity.ok(resultLog.append("\nTotal successes: ").append(successCount).toString());
    }
}
