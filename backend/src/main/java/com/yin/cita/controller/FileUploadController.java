package com.yin.cita.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yin.cita.model.DocumentElement;
import com.yin.cita.service.ChunkingService;
import com.yin.cita.service.FileParserService;
import com.yin.cita.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileParserService fileParserService;
    private final ChunkingService chunkingService;
    private final VectorStoreService vectorStoreService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FileUploadController(FileParserService fileParserService, ChunkingService chunkingService,
            VectorStoreService vectorStoreService) {
        this.fileParserService = fileParserService;
        this.chunkingService = chunkingService;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("files") List<MultipartFile> files) {
        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body("No files uploaded");
        }

        StringBuilder resultLog = new StringBuilder("Processed files:\n");
        int successCount = 0;

        for (MultipartFile file : files) {
            System.out.println("Processing file: " + file.getOriginalFilename());
            try {
                // 1. Parse File to Elements
                List<DocumentElement> elements = fileParserService.parseFileToElements(file);
                System.out.println("Parsed " + elements.size() + " elements.");

                // 2. Save Parsed Elements (JSON)
                String elementsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(elements);
                fileParserService.saveToLocal(file.getOriginalFilename(), elementsJson);

                // 3. Chunk Elements (WITH PARALLEL ENRICHMENT in service)
                List<Map<String, Object>> chunks = chunkingService.chunkElements(elements);
                System.out.println("Generated " + chunks.size() + " chunks.");

                // 4. Save Chunks
                chunkingService.saveChunks(file.getOriginalFilename(), chunks);

                // 5. Store in Pinecone
                vectorStoreService.storeChunks(chunks);

                resultLog.append("- ").append(file.getOriginalFilename()).append(": Success (").append(chunks.size())
                        .append(" chunks)\n");
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
