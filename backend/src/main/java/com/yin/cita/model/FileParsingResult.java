package com.yin.cita.model;

import java.util.List;
import java.util.Map;

public class FileParsingResult {
    private final List<DocumentElement> elements;
    private final Map<String, String> metadata;
    private final String content; // Full text content

    public FileParsingResult(List<DocumentElement> elements, Map<String, String> metadata, String content) {
        this.elements = elements;
        this.metadata = metadata;
        this.content = content;
    }

    public List<DocumentElement> getElements() {
        return elements;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getContent() {
        return content;
    }
}
