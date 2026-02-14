package com.yin.cita.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class Document {

    private Long id;
    private String filename;
    private String title;
    private String author;
    private String publicationDate;
    private LocalDateTime uploadDate = LocalDateTime.now();
    private String content;
    private String fileHash;
    private String processingStatus = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED
    private int processingProgress = 0; // 0-100
    private String errorMessage;
    private Set<Collection> collections = new HashSet<>();

    public Document(String filename, String author) {
        this.uploadDate = LocalDateTime.now();
        this.filename = filename;
        this.author = author;
    }
}
