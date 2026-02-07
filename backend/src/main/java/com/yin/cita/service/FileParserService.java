
package com.yin.cita.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class FileParserService {

    private final Tika tika = new Tika();

    public String parseFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null)
            filename = "unknown";
        String extension = getExtension(filename).toLowerCase();

        // 1. Handle Markdown directly (preserve content)
        if (extension.equals("md") || extension.equals("markdown")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        // 2. Handle Code/JSON (wrap in code block)
        if (extension.equals("json") || extension.equals("xml") || extension.equals("yaml")
                || extension.equals("yml")) {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return "```" + extension + "\n" + content + "\n```";
        }

        if (extension.equals("txt")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        // 3. Handle Binary/Document formats (PDF, DOC/X, PPT/X, HTML) using Tika
        // Tika auto-detects based on stream
        try {
            String content = tika.parseToString(file.getInputStream());
            return formatToMarkdown(filename, content);
        } catch (TikaException e) {
            throw new IOException("Failed to parse file: " + e.getMessage(), e);
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }

    private String formatToMarkdown(String filename, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(filename).append("\n\n");
        // Simple cleanup: ensure paragraphs are separated
        sb.append(content.trim());
        return sb.toString();
    }

    public void saveToLocal(String filename, String content) throws IOException {
        java.nio.file.Path directory = java.nio.file.Paths.get("data/parsed");
        if (!java.nio.file.Files.exists(directory)) {
            java.nio.file.Files.createDirectories(directory);
        }

        // Ensure filename ends with .md
        String savedFilename = filename;
        if (!savedFilename.toLowerCase().endsWith(".md")) {
            savedFilename += ".md";
        }

        java.nio.file.Path file = directory.resolve(savedFilename);
        java.nio.file.Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Saved parsed file to: " + file.toAbsolutePath());
    }
}
