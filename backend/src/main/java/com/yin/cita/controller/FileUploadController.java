
package com.yin.cita.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        System.out.println("Received file: " + file.getOriginalFilename());
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            System.out.println("Content: " + content);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error reading file content");
        }

        return ResponseEntity.ok("File uploaded successfully");
    }
}
