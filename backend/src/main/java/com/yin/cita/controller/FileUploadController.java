
package com.yin.cita.controller;

import com.yin.cita.service.FileParserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final FileParserService fileParserService;

    @Autowired
    public FileUploadController(FileParserService fileParserService) {
        this.fileParserService = fileParserService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        System.out.println("Received file: " + file.getOriginalFilename());
        try {
            String content = fileParserService.parseFile(file);
            System.out.println("Parsed Content Length: " + content.length());

            // Save to local file
            fileParserService.saveToLocal(file.getOriginalFilename(), content);

            return ResponseEntity.ok("File uploaded, parsed, and saved locally. Content length: " + content.length());
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error parsing file content: " + e.getMessage());
        }
    }
}
