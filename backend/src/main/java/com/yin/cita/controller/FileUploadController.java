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

import java.io.IOException;
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
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        System.out.println("Received file: " + file.getOriginalFilename());
        try {
            // 1. Parse File to Elements
            List<DocumentElement> elements = fileParserService.parseFileToElements(file);
            System.out.println("Parsed " + elements.size() + " elements.");

            // 2. Save Parsed Elements (JSON)
            String elementsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(elements);
            fileParserService.saveToLocal(file.getOriginalFilename(), elementsJson);

            // 3. Chunk Elements
            List<Map<String, Object>> chunks = chunkingService.chunkElements(elements);
            System.out.println("Generated " + chunks.size() + " chunks.");

            // 4. Save Chunks
            chunkingService.saveChunks(file.getOriginalFilename(), chunks);

            // 5. Store in Pinecone
            vectorStoreService.storeChunks(chunks);

            return ResponseEntity
                    .ok("File uploaded, parsed, saved, and chunked successfully. Elements: " + elements.size()
                            + ", Chunks: " + chunks.size());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error processing file: " + e.getMessage());
        }
    }
}
