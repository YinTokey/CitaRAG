package com.yin.cita.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
public class Collection {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();
    private Set<Document> documents = new HashSet<>();

    public Collection(String name) {
        this.createdAt = LocalDateTime.now();
        this.name = name;
    }

    public void addDocument(Document document) {
        this.documents.add(document);
        document.getCollections().add(this);
    }

    public void removeDocument(Document document) {
        this.documents.remove(document);
        document.getCollections().remove(this);
    }
}
