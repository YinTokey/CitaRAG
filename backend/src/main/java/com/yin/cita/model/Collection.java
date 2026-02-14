package com.yin.cita.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany
    @JoinTable(name = "collection_documents", joinColumns = @JoinColumn(name = "collection_id"), inverseJoinColumns = @JoinColumn(name = "document_id"))
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
