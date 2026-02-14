package com.yin.cita.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Schema(description = "Represents a document uploaded to the system.")
public class Document {

    @Schema(description = "Unique ID of the document", example = "1")
    private Long id;

    @Schema(description = "Original filename", example = "report.pdf")
    private String filename;

    @Schema(description = "Extracted title", example = "Quarterly Report 2024")
    private String title;

    @Schema(description = "Extracted author", example = "John Doe")
    private String author;

    @Schema(description = "Publication date (extracted)", example = "2024-01-01")
    private String publicationDate;

    @Schema(description = "Date of upload")
    private LocalDateTime uploadDate = LocalDateTime.now();

    @Schema(description = "Full text content of the document")
    private String content;

    @Schema(description = "SHA-256 hash of the file content")
    private String fileHash;

    @Schema(description = "Current processing status", example = "COMPLETED", allowableValues = { "PENDING",
            "PROCESSING", "COMPLETED", "FAILED" })
    private String processingStatus = "PENDING";

    @Schema(description = "Processing progress (0-100)", example = "100")
    private int processingProgress = 0;

    @Schema(description = "Error message if processing failed")
    private String errorMessage;

    @Schema(description = "Collections this document belongs to")
    private Set<Collection> collections = new HashSet<>();

    public Document(String filename, String author) {
        this.uploadDate = LocalDateTime.now();
        this.filename = filename;
        this.author = author;
    }
}
